/** Spring Boot Actuator health status. */
export type ReadinessStatus = "UP" | "DOWN";

export interface Readiness {
  status: ReadinessStatus;
}

export class ApiUnreachableError extends Error {
  constructor() {
    super("The IskolarHub API could not be reached.");
    this.name = "ApiUnreachableError";
  }
}

/** 200 (UP) and 503 (DOWN) both carry a Readiness body; anything else, or a network failure, is "unreachable". */
export async function fetchReadiness(signal?: AbortSignal): Promise<Readiness> {
  let res: Response;
  try {
    res = await fetch("/api/v1/actuator/health/readiness", { signal });
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") throw err;
    throw new ApiUnreachableError();
  }
  if (res.status !== 200 && res.status !== 503) throw new ApiUnreachableError();
  return (await res.json()) as Readiness;
}
