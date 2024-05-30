/*
 * Copyright (c) Mihir Paldhikar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import { NextRequest, NextResponse } from "next/server";

const protectedRoutes: Array<string> = ["/"];

function parseCookie(cookieArray: string[]): Record<string, string> {
  const cookieObject: Record<string, string> = {};

  for (const cookieString of cookieArray) {
    const [key, ...valueParts] = cookieString.split("=");
    const value = valueParts.join("=");

    if (key.toLowerCase() !== "httponly") {
      cookieObject[key] = value;
    }
  }

  return cookieObject;
}

export default async function middleware(req: NextRequest) {
  if (
    req.nextUrl.pathname.includes("/auth") ||
    req.nextUrl.pathname.includes("/")
  ) {
    const authenticationResponse = await fetch(
      `${process.env.NEXT_PUBLIC_AUTH_SERVICE_URL}/accounts/`,
      {
        credentials: "same-origin",
        method: "GET",
        headers: {
          Cookie: req.cookies.toString(),
        },
      },
    );

    if (
      req.nextUrl.pathname.startsWith("/auth") &&
      authenticationResponse.status === 200
    ) {
      const absoluteURL = new URL("/", req.nextUrl.origin);
      return NextResponse.redirect(absoluteURL.toString());
    }

    if (
      authenticationResponse.status !== 200 &&
      protectedRoutes.includes(req.nextUrl.pathname)
    ) {
      if (req.cookies.has("__rt__") && !req.cookies.has("__at__")) {
        const refreshSecurityTokenResponse = await fetch(
          `${process.env.NEXT_PUBLIC_AUTH_SERVICE_URL}/accounts/refresh`,
          {
            credentials: "same-origin",
            method: "POST",
            headers: {
              Cookie: req.cookies.toString(),
            },
          },
        );

        if (refreshSecurityTokenResponse.status !== 200) {
          const absoluteURL = new URL("/auth/signin", req.nextUrl.origin);
          return NextResponse.redirect(absoluteURL.toString());
        }

        const cookies = refreshSecurityTokenResponse.headers
          .getSetCookie()
          .toString()
          .split(", __");

        const nextResponse = NextResponse.next();

        const authorizationTokenCookie = parseCookie(cookies[0].split(";"));
        const refreshTokenCookie = parseCookie(cookies[1].split(";"));

        nextResponse.cookies
          .set("__at__", authorizationTokenCookie["__at__"], {
            expires: Date.now() + 3600000,
            domain: authorizationTokenCookie["Domain"],
            path: authorizationTokenCookie["Path"],
            sameSite: "lax",
            httpOnly: true,
          })
          .set("__rt__", refreshTokenCookie["rt__"], {
            expires: Date.now() + 2629800000,
            domain: refreshTokenCookie["Domain"],
            path: refreshTokenCookie["Path"],
            sameSite: "lax",
            httpOnly: true,
          });

        return nextResponse;
      } else {
        const absoluteURL = new URL("/auth/signin", req.nextUrl.origin);
        return NextResponse.redirect(absoluteURL.toString());
      }
    }
  } else {
    const requestHeaders = new Headers(req.headers);
    requestHeaders.set("x-pathname", req.nextUrl.pathname);
    return NextResponse.next({
      request: {
        headers: requestHeaders,
      },
    });
  }
}
