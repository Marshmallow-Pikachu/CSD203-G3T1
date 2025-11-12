import Logo from "../../assets/google.svg?react";

const rawBase = import.meta.env.VITE_API_BASE;

function buildAuthUrl() {
  if (!rawBase) return "/oauth2/authorization/google"; // same-origin fallback
  return new URL("/oauth2/authorization/google", rawBase).toString();
}

export default function SocialAuthButtons() {
  const GOOGLE_AUTH_URL = buildAuthUrl();

  // TEMP debug
  console.log("VITE_API_BASE =", rawBase);
  console.log("GOOGLE_AUTH_URL =", GOOGLE_AUTH_URL);

  return (
    <div className="space-y-2">
      <a
        href={GOOGLE_AUTH_URL}
        className="w-full block bg-white border border-gray-300 text-gray-700 rounded-lg py-2 hover:bg-gray-50 text-center relative"
        data-href={GOOGLE_AUTH_URL} // also visible in Elements panel
      >
        <span className="absolute left-4 top-1/2 -translate-y-1/2">
          <Logo className="size-8" />
        </span>
        Continue with Google
      </a>
    </div>
  );
}
