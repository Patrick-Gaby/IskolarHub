import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

function mockFetch(impl: () => Promise<Response>) {
  vi.stubGlobal("fetch", vi.fn(impl));
}

afterEach(() => vi.unstubAllGlobals());

describe("App service status", () => {
  it("shows ready when the API reports ready", async () => {
    mockFetch(async () =>
      new Response(JSON.stringify({ status: "UP" }), { status: 200 }),
    );
    render(<App />);
    expect(await screen.findByRole("status")).toHaveTextContent(/^Ready/);
  });

  it("shows degraded on a 503 readiness body", async () => {
    mockFetch(async () =>
      new Response(JSON.stringify({ status: "DOWN" }), { status: 503 }),
    );
    render(<App />);
    expect(await screen.findByRole("status")).toHaveTextContent(/^Degraded/);
  });

  it("shows unavailable when the network fails", async () => {
    mockFetch(async () => {
      throw new TypeError("network down");
    });
    render(<App />);
    expect(await screen.findByRole("status")).toHaveTextContent(/^Unavailable/);
  });
});
