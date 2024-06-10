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

"use client";
import { Fragment, JSX, useState } from "react";
import { AuthService } from "@services/index";
import { StatusCode } from "@enums/StatusCode";
import { useRouter } from "next/navigation";
import { PasskeyIcon } from "@icons/index";

const authService = new AuthService();

export default function SignInForm(): JSX.Element {
  const router = useRouter();

  const [identifier, setIdentifier] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [authenticationStrategy, setAuthenticationStrategy] = useState<
    "UNKNOWN" | "PASSWORD" | "PASSKEY"
  >("UNKNOWN");

  return (
    <form
      className={"w-full space-y-3"}
      onSubmit={async (event) => {
        event.preventDefault();
        setSubmitting(true);
        switch (authenticationStrategy) {
          case "UNKNOWN": {
            const response =
              await authService.getAuthenticationStrategy(identifier);
            if (
              response.statusCode === StatusCode.SUCCESS &&
              "payload" in response
            ) {
              setAuthenticationStrategy(
                response.payload as "UNKNOWN" | "PASSWORD" | "PASSKEY",
              );
            }
            break;
          }
          case "PASSKEY": {
            const response = await authService.authenticate(
              authenticationStrategy,
              identifier,
            );
            if (response.statusCode === StatusCode.AUTHENTICATION_SUCCESSFUL) {
              router.replace("/");
            }
            break;
          }
          case "PASSWORD": {
            const response = await authService.authenticate(
              authenticationStrategy,
              identifier,
              password,
            );
            if (response.statusCode === StatusCode.AUTHENTICATION_SUCCESSFUL) {
              router.replace("/");
            }
            break;
          }
          default:
            break;
        }
        setSubmitting(false);
      }}
    >
      <input
        className={"w-full rounded-md border px-3 py-2"}
        disabled={submitting}
        type={"text"}
        value={identifier}
        placeholder={"Username..."}
        onChange={(event) => {
          setIdentifier(event.target.value);
        }}
      />
      <input
        hidden={!(authenticationStrategy === "PASSWORD")}
        className={"w-full rounded-md border px-3 py-2"}
        disabled={submitting}
        type={"password"}
        value={password}
        placeholder={"Password..."}
        onChange={(event) => {
          setPassword(event.target.value);
        }}
      />
      <button
        disabled={submitting || identifier.length === 0}
        type={"submit"}
        className={
          "w-full rounded-md bg-blue-600 px-3 py-2 font-semibold text-white disabled:bg-gray-300 disabled:text-gray-500"
        }
      >
        {authenticationStrategy === "PASSKEY" ? (
          <Fragment>
            <PasskeyIcon color={submitting ? "#6b7280" : "#ffffff"} />{" "}
            <h2>Continue With Passkey</h2>
          </Fragment>
        ) : (
          <h2>Continue</h2>
        )}
      </button>
    </form>
  );
}
