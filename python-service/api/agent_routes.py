from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from workflows.knowledge_qa_agent import KnowledgeQAAgent
from agent.state import AgentState
from agent.events import event_bus, Event
from typing import Optional, List, Dict, Any
import time
import logging
import json
import uuid

logger = logging.getLogger(__name__)

router = APIRouter()

agent = KnowledgeQAAgent()


class AgentRunRequest(BaseModel):
    input: str
    conversation_id: Optional[str] = None
    user_id: Optional[str] = None
    context: Optional[str] = ""
    goal: Optional[str] = None
    run_id: Optional[str] = None
    trace_id: Optional[str] = None
    stream: bool = False


class AgentRunResponse(BaseModel):
    run_id: str
    trace_id: str
    status: str
    answer: str
    sources: List[Dict[str, Any]]
    steps: List[Dict[str, Any]]
    tool_calls: List[Dict[str, Any]]
    intermediate_conclusions: List[Dict[str, Any]]


class StepEvent(BaseModel):
    run_id: str
    step_id: str
    step_name: str
    step_type: str
    status: str


class ToolCallEvent(BaseModel):
    run_id: str
    tool_call_id: str
    tool_name: str
    status: str
    duration_ms: Optional[float] = None


stored_states: Dict[str, AgentState] = {}


def on_run_completed(event: Event):
    """处理运行完成事件"""
    if hasattr(event, 'data') and 'output' in event.data:
        run_id = event.run_id
        logger.info(f"[Event] Run {run_id} completed")


def on_step_completed(event: Event):
    """处理步骤完成事件"""
    if event.data.get('step_name') == 'answer_generation':
        run_id = event.run_id
        logger.info(f"[Event] Step answer_generation completed for run {run_id}")


event_bus.subscribe("run_completed", on_run_completed)
event_bus.subscribe("step_completed", on_step_completed)


@router.post("/agent/run")
async def run_agent(request: AgentRunRequest):
    """
    执行Agent

    Args:
        request: Agent运行请求

    Returns:
        Agent执行结果
    """
    start_time = time.time()

    try:
        logger.info(f"[Agent API] Received run request: {request.input[:50]}...")

        result = agent.ask(
            question=request.input,
            conversation_id=request.conversation_id,
            user_id=request.user_id,
            context=request.context or "",
            goal=request.goal,
            run_id=request.run_id,
            trace_id=request.trace_id
        )

        run_id = request.run_id or str(uuid.uuid4())
        trace_id = request.trace_id or str(uuid.uuid4())

        response = {
            "run_id": run_id,
            "trace_id": trace_id,
            "status": "completed",
            "answer": result.get("answer", ""),
            "sources": result.get("sources", []),
            "steps": [],
            "tool_calls": [],
            "intermediate_conclusions": []
        }

        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/agent/run",
                "status_code": 200,
                "process_time": process_time,
                "run_id": run_id
            })
        )

        return response

    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"[Agent API] Error running agent: {str(e)}")
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/agent/run",
                "status_code": 500,
                "process_time": process_time
            })
        )
        raise HTTPException(status_code=500, detail=f"Agent执行失败: {str(e)}")


@router.post("/agent/run/stream")
async def run_agent_stream(request: AgentRunRequest):
    """
    流式执行Agent (Server-Sent Events)

    Args:
        request: Agent运行请求

    Returns:
        SSE事件流
    """
    async def event_generator():
        try:
            logger.info(f"[Agent API] Stream request: {request.input[:50]}...")

            for event_data in agent.ask_stream(
                question=request.input,
                conversation_id=request.conversation_id,
                user_id=request.user_id,
                context=request.context or "",
                goal=request.goal,
                run_id=request.run_id,
                trace_id=request.trace_id
            ):
                yield f"data: {event_data}\n\n"

            yield f"data: {json.dumps({'type': 'complete'})}\n\n"

        except Exception as e:
            logger.error(f"[Agent API] Stream error: {str(e)}")
            yield f"data: {json.dumps({'type': 'error', 'content': str(e)})}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )


@router.get("/agent/run/{run_id}")
async def get_run_status(run_id: str):
    """
    获取Agent运行状态

    Args:
        run_id: 运行ID

    Returns:
        运行状态信息
    """
    if run_id in stored_states:
        state = stored_states[run_id]
        return {
            "run_id": state.run_id,
            "trace_id": state.trace_id,
            "status": state.status.value,
            "current_step_index": state.current_step_index,
            "steps": [
                {
                    "step_id": s.step_id,
                    "step_name": s.step_name,
                    "status": s.status.value,
                    "duration_ms": s.duration_ms
                }
                for s in state.steps
            ],
            "elapsed_time": state.elapsed_time
        }

    return {
        "run_id": run_id,
        "status": "not_found",
        "message": "Run not found or expired"
    }


@router.get("/agent/run/{run_id}/steps")
async def get_run_steps(run_id: str):
    """
    获取Agent运行步骤详情

    Args:
        run_id: 运行ID

    Returns:
        步骤详情列表
    """
    if run_id not in stored_states:
        raise HTTPException(status_code=404, detail="Run not found")

    state = stored_states[run_id]
    return {
        "run_id": run_id,
        "steps": [
            {
                "step_id": s.step_id,
                "step_type": s.step_type.value,
                "step_name": s.step_name,
                "status": s.status.value,
                "input_data": s.input_data,
                "output_data": s.output_data,
                "error_message": s.error_message,
                "duration_ms": s.duration_ms,
                "tool_call_id": s.tool_call_id,
                "start_time": s.start_time,
                "end_time": s.end_time
            }
            for s in state.steps
        ]
    }


@router.get("/agent/run/{run_id}/tool-calls")
async def get_run_tool_calls(run_id: str):
    """
    获取Agent运行的工具调用记录

    Args:
        run_id: 运行ID

    Returns:
        工具调用记录列表
    """
    if run_id not in stored_states:
        raise HTTPException(status_code=404, detail="Run not found")

    state = stored_states[run_id]
    return {
        "run_id": run_id,
        "tool_calls": [
            {
                "tool_call_id": tc.tool_call_id,
                "tool_name": tc.tool_name,
                "input_params": tc.input_params,
                "output": tc.output,
                "status": tc.status,
                "duration_ms": tc.duration_ms,
                "error_message": tc.error_message,
                "timestamp": tc.timestamp
            }
            for tc in state.tool_calls
        ]
    }


@router.get("/agent/tools")
async def list_tools():
    """
    列出所有可用的Agent工具

    Returns:
        工具列表
    """
    from tools.registry import tool_registry

    tools = tool_registry.get_all_tools()
    return {
        "tools": [
            tool_registry.get_tool_info(name)
            for name in tools.keys()
        ]
    }


@router.get("/health")
async def health_check():
    """
    健康检查

    Returns:
        健康状态
    """
    return {
        "status": "healthy",
        "service": "agent-service",
        "timestamp": time.time()
    }
