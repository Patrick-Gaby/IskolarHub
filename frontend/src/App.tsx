import { useEffect, useState } from "react";
import { fetchReadiness, type Readiness } from "./api/client";

type State =
  | { kind: "loading" }
  | { kind: "done"; readiness: Readiness }
  | { kind: "unreachable" };

export default function App() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    const controller = new AbortController();
    fetchReadiness(controller.signal)
      .then((readiness) => setState({ kind: "done", readiness }))
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === "AbortError") return;
        setState({ kind: "unreachable" });
      });
    return () => controller.abort();
  }, []);

  return (
    <main className="mx-auto max-w-2xl px-4 py-10 animate-fade-in">
      <h1 className="text-3xl font-bold text-primary-800">IskolarHub</h1>
      <p className="mt-2 text-slate-700">
        Find Philippine scholarships you qualify for. Application pages are being built.
      </p>
      <section aria-labelledby="status-heading" className="mt-8 rounded-2xl border border-slate-200 p-4">
        <h2 id="status-heading" className="text-lg font-semibold">
          Service status
        </h2>
        <p role="status" className="mt-1 text-slate-800">
          {state.kind === "loading" && "Checking the service…"}
          {state.kind === "unreachable" && "Unavailable: the API could not be reached."}
          {state.kind === "done" &&
            (state.readiness.status === "UP"
              ? "Ready: the API and database are working."
              : "Degraded: the API is running but the database is not available.")}
        </p>
      </section>
    </main>
  );
}
