import { ApiError } from "@/lib/api"

export function getApiErrorMessage(
  err: unknown,
  fallback = "Something went wrong"
): string {
  if (!(err instanceof ApiError)) {
    if (err instanceof Error && err.message) {
      return err.message
    }
    return fallback
  }

  const body = err.body?.trim()
  if (!body) {
    return err.message || fallback
  }

  try {
    const json = JSON.parse(body) as {
      detail?: string
      message?: string
      title?: string
      error?: string
      status?: number
      path?: string
    }
    if (json.detail || json.message || json.title) {
      return json.detail || json.message || json.title || fallback
    }
    if (json.status === 500 || json.error === "Internal Server Error") {
      return "Service is starting or unavailable. Please retry in a few seconds."
    }
    return json.error || fallback
  } catch {
    return body
  }
}
