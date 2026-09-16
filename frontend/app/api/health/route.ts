import { NextResponse } from "next/server";

export async function GET() {
  try {
    const response = await fetch("http://localhost:8080/api/health");

    if (!response.ok) {
      return NextResponse.json(
        { status: "DOWN" },
        { status: response.status }
      );
    }

    const data = await response.json();

    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { status: "DOWN" },
      { status: 503 }
    );
  }
}