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
import { AuthService } from "@services/index";
import { JSX, useState } from "react";
import { useRouter } from "next/navigation";
import { StatusCode } from "@enums/StatusCode";

const authService = new AuthService();

export default function SignUpForm(): JSX.Element {
  const router = useRouter();

  const [identifier, setIdentifier] = useState<string>("");
  const [displayName, setDisplayName] = useState<string>("");
  const [email, setEmail] = useState<string>("");
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [authenticationStrategy, setAuthenticationStrategy] = useState<
    "PASSKEY" | "PASSWORD"
  >("PASSKEY");

  return (
    <form
      className={"w-full space-y-3"}
      onSubmit={async (event) => {
        event.preventDefault();
        setSubmitting(true);
        const result = await authService.createAccount(
          authenticationStrategy,
          displayName,
          identifier,
          email,
        );
        if (
          result.statusCode === StatusCode.PASSKEY_REGISTERED ||
          result.statusCode === StatusCode.AUTHENTICATION_SUCCESSFUL
        ) {
          router.push("/");
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
        className={"w-full rounded-md border px-3 py-2"}
        disabled={submitting}
        type={"text"}
        value={displayName}
        placeholder={"Name..."}
        onChange={(event) => {
          setDisplayName(event.target.value);
        }}
      />
      <input
        className={"w-full rounded-md border px-3 py-2"}
        disabled={submitting}
        type={"email"}
        value={email}
        placeholder={"Email..."}
        onChange={(event) => {
          setEmail(event.target.value);
        }}
      />
      <button
        disabled={
          identifier.length === 0 ||
          displayName.length === 0 ||
          email.length === 0 ||
          submitting
        }
        type={"submit"}
        className={
          "w-full rounded-md bg-blue-600 px-3 py-2 font-semibold text-white disabled:bg-gray-300 disabled:text-gray-500"
        }
      >
        Continue
      </button>
    </form>
  );
}
