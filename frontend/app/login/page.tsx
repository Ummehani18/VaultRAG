"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Lock, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { login, register } from "@/lib/api";

type Mode = "login" | "register";

export default function LoginPage() {
  const router = useRouter();

  const [mode, setMode] = useState<Mode>("login");

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] =
    useState("");

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  function switchMode(nextMode: Mode) {
    setMode(nextMode);
    setError("");
    setSuccess("");
    setUsername("");
    setPassword("");
    setConfirmPassword("");
  }

  async function handleLogin(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setError("");
    setSuccess("");
    setLoading(true);

    try {
      const response = await login({
        username: username.trim(),
        password,
      });

      sessionStorage.setItem(
        "vaultrag_token",
        response.token
      );

      sessionStorage.setItem(
        "vaultrag_user",
        JSON.stringify({
          id: response.id,
          username: response.username,
          role: response.role,
        })
      );

      router.push("/ask");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Login failed"
      );
    } finally {
      setLoading(false);
    }
  }

  async function handleRegister(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setError("");
    setSuccess("");

    const normalizedUsername =
      username.trim();

    if (!normalizedUsername) {
      setError("Username is required.");
      return;
    }

    if (normalizedUsername.length < 3) {
      setError(
        "Username must be at least 3 characters."
      );
      return;
    }

    if (!password) {
      setError("Password is required.");
      return;
    }

    if (password.length < 8) {
      setError(
        "Password must be at least 8 characters."
      );
      return;
    }

    if (password !== confirmPassword) {
      setError(
        "Passwords do not match."
      );
      return;
    }

    setLoading(true);

    try {
      await register({
        username: normalizedUsername,
        password,
      });

      /*
       * Registration does not automatically log
       * the user in. The user signs in normally so
       * JWT creation remains in one authentication flow.
       */
      setSuccess(
        "Account created successfully. You can now sign in."
      );

      setPassword("");
      setConfirmPassword("");

      setTimeout(() => {
        setMode("login");
        setSuccess("");
      }, 1200);

    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Account creation failed"
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen grid grid-cols-1 md:grid-cols-2 bg-vault-bg text-vault-text">

      {/* Editorial side */}
      <div className="hidden md:flex flex-col justify-between p-12 border-r border-vault-border/15">

        <div className="flex items-center gap-2">
          <Lock
            size={16}
            strokeWidth={2.5}
          />

          <span className="font-medium tracking-tight">
            VaultRAG
          </span>
        </div>

        <div className="max-w-sm">

          <p className="text-xs font-mono tracking-wide text-vault-muted mb-3">
            PRIVATE DOCUMENT INTELLIGENCE WORKSPACE
          </p>

          <p className="font-serif text-3xl leading-snug">
            Answers grounded in the documents your organization already trusts.
          </p>

          <p className="text-sm text-vault-muted mt-4">
            Every response cites the policy, page, and section it came from.
            Nothing leaves your controlled environment.
          </p>

        </div>

        <p className="text-xs text-vault-muted font-mono">
          HEALTHCARE FINANCE · INSURANCE OPERATIONS
        </p>

      </div>

      {/* Authentication card */}
      <div className="flex items-center justify-center p-8">

        <div className="w-full max-w-sm border border-vault-border/15 rounded-swiss bg-vault-surface p-7">

          {/* Mode selector */}
          <div className="flex bg-vault-secondary rounded-swiss p-1 mb-7 text-sm">

            <button
              type="button"
              onClick={() => switchMode("login")}
              className={`flex-1 rounded-[7px] py-2 font-medium transition-colors ${
                mode === "login"
                  ? "bg-vault-surface text-vault-text shadow-brutal-sm border border-vault-border"
                  : "text-vault-muted hover:text-vault-text"
              }`}
            >
              Sign in
            </button>

            <button
              type="button"
              onClick={() => switchMode("register")}
              className={`flex-1 rounded-[7px] py-2 font-medium transition-colors ${
                mode === "register"
                  ? "bg-vault-surface text-vault-text shadow-brutal-sm border border-vault-border"
                  : "text-vault-muted hover:text-vault-text"
              }`}
            >
              Create account
            </button>

          </div>

          {mode === "login" ? (

            <>
              <h1 className="text-xl font-medium mb-1">
                Sign in
              </h1>

              <p className="text-sm text-vault-muted mb-6">
                Access your secure document workspace
              </p>

              <form
                className="space-y-4"
                onSubmit={handleLogin}
              >

                <Field
                  label="Username"
                  type="text"
                  placeholder="Enter username"
                  value={username}
                  onChange={setUsername}
                />

                <Field
                  label="Password"
                  type="password"
                  placeholder="••••••••••"
                  value={password}
                  onChange={setPassword}
                />

                {error && (
                  <div className="bg-vault-error-bg border border-vault-error rounded-swiss px-3 py-2 text-sm text-vault-error">
                    {error}
                  </div>
                )}

                {success && (
                  <div className="border border-vault-border rounded-swiss px-3 py-2 text-sm">
                    {success}
                  </div>
                )}

                <div className="flex items-center justify-between text-xs pt-1">

                  <label className="flex items-center gap-2 text-vault-muted">

                    <input
                      type="checkbox"
                      className="accent-vault-text"
                    />

                    Stay signed in

                  </label>

                  <button
                    type="button"
                    className="text-vault-muted hover:text-vault-text"
                  >
                    Forgot password?
                  </button>

                </div>

                <div className="pt-2">

                  <Button
                    variant="brutal"
                    fullWidth
                    type="submit"
                    disabled={loading}
                  >
                    {loading
                      ? "Signing in..."
                      : "Sign in"}
                  </Button>

                </div>

              </form>

              <p className="text-xs text-vault-muted mt-6">
                Access is limited to authorized members of your organization.{" "}

                <button
                  type="button"
                  onClick={() =>
                    switchMode("register")
                  }
                  className="text-vault-text underline underline-offset-2"
                >
                  Request access
                </button>
              </p>

            </>

          ) : (

            <>
              <h1 className="text-xl font-medium mb-1">
                Create account
              </h1>

              <p className="text-sm text-vault-muted mb-6">
                Set up access to your organization&apos;s workspace
              </p>

              <form
                className="space-y-4"
                onSubmit={handleRegister}
              >

                <Field
                  label="Username"
                  type="text"
                  placeholder="Choose username"
                  value={username}
                  onChange={setUsername}
                />

                <Field
                  label="Password"
                  type="password"
                  placeholder="Enter password"
                  value={password}
                  onChange={setPassword}
                />

                <Field
                  label="Confirm password"
                  type="password"
                  placeholder="••••••••••"
                  value={confirmPassword}
                  onChange={setConfirmPassword}
                />

                {error && (
                  <div className="bg-vault-error-bg border border-vault-error rounded-swiss px-3 py-2 text-sm text-vault-error">
                    {error}
                  </div>
                )}

                {success && (
                  <div className="border border-vault-border rounded-swiss px-3 py-2 text-sm">
                    {success}
                  </div>
                )}

                <Button
                  variant="brutal"
                  fullWidth
                  type="submit"
                  disabled={loading}
                >
                  {loading
                    ? "Creating account..."
                    : "Create account"}
                </Button>

              </form>

              <p className="text-xs text-vault-muted mt-6">
                Already have access?{" "}

                <button
                  type="button"
                  onClick={() =>
                    switchMode("login")
                  }
                  className="text-vault-text underline underline-offset-2"
                >
                  Sign in instead
                </button>
              </p>

            </>
          )}

        </div>
      </div>
    </div>
  );
}

function Field({
  label,
  type,
  placeholder,
  value,
  onChange,
}: {
  label: string;
  type: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
}) {
  const [showPassword, setShowPassword] = useState(false);

  const isPasswordField = type === "password";

  const inputType =
    isPasswordField && showPassword ? "text" : type;

  return (
    <div>
      <label className="text-xs text-vault-muted block mb-1.5">
        {label}
      </label>

      <div className="relative">
        <input
          type={inputType}
          placeholder={placeholder}
          value={value}
          onChange={(event) =>
            onChange(event.target.value)
          }
          required
          className={`w-full border border-vault-border/25 rounded-swiss px-3 py-2.5 text-sm bg-vault-surface outline-none focus:border-vault-border ${
            isPasswordField ? "pr-10" : ""
          }`}
        />

        {isPasswordField && (
          <button
            type="button"
            onClick={() =>
              setShowPassword((current) => !current)
            }
            className="absolute right-3 top-1/2 -translate-y-1/2 text-vault-muted hover:text-vault-text transition-colors"
            aria-label={
              showPassword
                ? "Hide password"
                : "Show password"
            }
          >
            {showPassword ? (
              <EyeOff size={17} strokeWidth={2} />
            ) : (
              <Eye size={17} strokeWidth={2} />
            )}
          </button>
        )}
      </div>
    </div>
  );
}