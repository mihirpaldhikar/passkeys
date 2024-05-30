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
import { Fragment, useEffect, useState } from "react";
import { Account } from "@dto/index";
import { StatusCode } from "@enums/StatusCode";
import { useRouter } from "next/navigation";

const authService = new AuthService();

export default function Home() {
  const router = useRouter();

  const [account, setAccount] = useState<Account | null>(null);

  useEffect(() => {
    authService.getAccount().then((response) => {
      if (
        response.statusCode === StatusCode.SUCCESS &&
        "payload" in response &&
        typeof response.payload === "object"
      ) {
        setAccount(response.payload);
      }
    });
  }, []);

  if (account === null) {
    return <Fragment />;
  }

  return (
    <section className={"flex min-h-dvh"}>
      <div className="m-auto space-y-3">
        <h4 className={"text-xl font-bold"}>Hello 👋🏻,</h4>
        <h1 className={"text-6xl font-black"}>{account.displayName}</h1>
        <h4 className={"font-medium"}>Email: {account.email}</h4>
        <button
          className={
            "rounded-full bg-red-600 px-10 py-2 font-semibold text-white"
          }
          onClick={async () => {
            const response = await authService.signOut();
            if (response.statusCode === StatusCode.SUCCESS) {
              router.refresh();
            }
          }}
        >
          Sign Out
        </button>
      </div>
    </section>
  );
}
