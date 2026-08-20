/**
 * An HTTP failure from the backend, carrying the status alongside the reason the server sent.
 *
 * Callers that only need something to show the player can use `message`; callers that need to
 * branch (403 vs 400, say) can read `status`.
 */
export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}
