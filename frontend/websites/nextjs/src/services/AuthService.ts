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

import {
  create as createPasskeyCredentials,
  get as getPublicCredentials,
} from "@github/webauthn-json";
import axios, { AxiosError, AxiosInstance } from "axios";
import { StatusCode } from "@enums/index";
import { Response } from "@dto/index";

export default class AuthService {
  private ACCOUNT_SERVICE_URL = (
    process.env.NEXT_PUBLIC_AUTH_SERVICE_URL as string
  ).concat("/accounts");

  private httpClient: AxiosInstance;
  private REQUEST_TIMEOUT = 1000 * 10;

  public constructor() {
    this.httpClient = axios.create({
      baseURL: this.ACCOUNT_SERVICE_URL,
      timeout: this.REQUEST_TIMEOUT,
      withCredentials: true,
    });
  }

  public async createAccount(
    authenticationStrategy: "PASSWORD" | "PASSKEY",
    displayName: string,
    username: string,
    email: string,
    password?: string,
  ): Promise<Response<string>> {
    try {
      const response = await this.httpClient.post(
        `${this.ACCOUNT_SERVICE_URL}/new`,
        password === undefined && authenticationStrategy === "PASSKEY"
          ? {
              username: username,
              displayName: displayName,
              email: email,
              authenticationStrategy: authenticationStrategy,
            }
          : {
              username: username,
              displayName: displayName,
              email: email,
              password: password,
              authenticationStrategy: authenticationStrategy,
            },
      );

      if (response.status === 200) {
        if (authenticationStrategy === "PASSKEY") {
          const passkeyChallenge = await response.data;

          const passkeyCredentials =
            await createPasskeyCredentials(passkeyChallenge);

          const challengeResponse = await this.httpClient.post(
            `${this.ACCOUNT_SERVICE_URL}/passkeys/validateRegistrationChallenge`,
            {
              identifier: username,
              passkeyCredentials: JSON.stringify(passkeyCredentials),
            },
          );

          if (challengeResponse.status === 201) {
            return {
              statusCode: StatusCode.PASSKEY_REGISTERED,
              message: "Passkey Registered.",
            } as Response<string>;
          }
        }
        return {
          statusCode: StatusCode.AUTHENTICATION_SUCCESSFUL,
          message: "Account Created Successfully.",
        } as Response<string>;
      }

      throw new AxiosError("INTERNAL:Account Creation Failed.");
    } catch (error) {
      let axiosError = (await error) as AxiosError;
      if (axiosError.message.includes("INTERNAL:")) {
        return {
          statusCode: StatusCode.AUTHENTICATION_FAILED,
          message: axiosError.message.replaceAll("INTERNAL:", ""),
        } as Response<string>;
      }

      let errorResponseString = JSON.stringify(
        (await axiosError.response?.data) as string,
      );
      let errorResponse = JSON.parse(errorResponseString);

      return {
        statusCode: StatusCode.AUTHENTICATION_FAILED,
        message: errorResponse["message"],
      } as Response<string>;
    }
  }

  public async getAuthenticationStrategy(
    identifier: string,
  ): Promise<Response<string>> {
    try {
      const response = await this.httpClient.post(
        `${this.ACCOUNT_SERVICE_URL}/authenticationStrategy`,
        {
          identifier: identifier,
        },
      );

      if (response.status === 200) {
        return {
          statusCode: StatusCode.SUCCESS,
          payload: response.data["authenticationStrategy"],
        } as Response<string>;
      }
      throw new AxiosError("INTERNAL:Request Failed.");
    } catch (error) {
      let axiosError = (await error) as AxiosError;
      if (axiosError.message.includes("INTERNAL:")) {
        return {
          statusCode: StatusCode.FAILURE,
          message: axiosError.message.replaceAll("INTERNAL:", ""),
        } as Response<string>;
      }

      let errorResponseString = JSON.stringify(
        (await axiosError.response?.data) as string,
      );
      let errorResponse = JSON.parse(errorResponseString);

      return {
        statusCode: StatusCode.FAILURE,
        message: errorResponse["message"],
      } as Response<string>;
    }
  }

  public async authenticate(
    authenticationStrategy: "PASSWORD" | "PASSKEY",
    identifier: string,
    password?: string,
  ): Promise<Response<string>> {
    try {
      const response = await this.httpClient.post(
        `${this.ACCOUNT_SERVICE_URL}/authenticate`,
        password === undefined
          ? {
              identifier: identifier,
            }
          : {
              identifier: identifier,
              password: password,
            },
      );

      if (authenticationStrategy === "PASSKEY") {
        const passkeyChallenge = await response.data;
        const passkeyCredentials = await getPublicCredentials(passkeyChallenge);

        const challengeResponse = await this.httpClient.post(
          `${this.ACCOUNT_SERVICE_URL}/passkeys/validatePasskeyChallenge`,
          {
            identifier: identifier,
            passkeyCredentials: JSON.stringify(passkeyCredentials),
          },
        );

        if (challengeResponse.status === 200) {
          return {
            statusCode: StatusCode.AUTHENTICATION_SUCCESSFUL,
            message: "Authenticated Successfully.",
          } as Response<string>;
        }
      }

      if (response.status === 200) {
        return {
          statusCode: StatusCode.AUTHENTICATION_SUCCESSFUL,
          message: "Authenticated Successfully.",
        } as Response<string>;
      }

      throw new AxiosError("INTERNAL:Authentication Failed.");
    } catch (error) {
      let axiosError = (await error) as AxiosError;
      if (axiosError.message.includes("INTERNAL:")) {
        return {
          statusCode: StatusCode.AUTHENTICATION_FAILED,
          message: axiosError.message.replaceAll("INTERNAL:", ""),
        } as Response<string>;
      }

      let errorResponseString = JSON.stringify(
        (await axiosError.response?.data) as string,
      );
      let errorResponse = JSON.parse(errorResponseString);

      return {
        statusCode: StatusCode.AUTHENTICATION_FAILED,
        message: errorResponse["message"],
      } as Response<string>;
    }
  }

  public async registerPasskey(identifier: string): Promise<Response<string>> {
    try {
      const response = await this.httpClient.post(
        `${this.ACCOUNT_SERVICE_URL}/passkeys/register`,
        {
          identifier: identifier,
        },
      );

      if (response.status === 200) {
        const passkeyChallenge = await response.data;
        const passkeyCredentials =
          await createPasskeyCredentials(passkeyChallenge);

        const challengeResponse = await this.httpClient.post(
          `${this.ACCOUNT_SERVICE_URL}/passkeys/validateRegistrationChallenge`,
          {
            identifier: identifier,
            passkeyCredentials: JSON.stringify(passkeyCredentials),
          },
        );

        if (challengeResponse.status === 201) {
          return {
            statusCode: StatusCode.PASSKEY_REGISTERED,
            message: "Passkey Registered.",
          } as Response<string>;
        }
      }

      throw new AxiosError("INTERNAL:Passkey Registration Failed.");
    } catch (error) {
      let axiosError = (await error) as AxiosError;
      if (axiosError.message.includes("INTERNAL:")) {
        return {
          statusCode: StatusCode.FAILURE,
          message: axiosError.message.replaceAll("INTERNAL:", ""),
        } as Response<string>;
      }

      let errorResponseString = JSON.stringify(
        (await axiosError.response?.data) as string,
      );
      let errorResponse = JSON.parse(errorResponseString);

      return {
        statusCode: StatusCode.FAILURE,
        message: errorResponse["message"],
      } as Response<string>;
    }
  }
}
