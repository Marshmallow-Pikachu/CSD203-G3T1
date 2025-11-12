import Logo from "../../assets/google.svg?react";

const GOOGLE_AUTH_URL = "http://localhost:8080/login/oauth2/code/google";

const go = (url: string) => {
  window.location.href = url;
};

export default function SocialAuthButtons() {
  return (
    <div className="space-y-2">

      <button
        type="button"
        onClick={() => go(GOOGLE_AUTH_URL)}
        className="w-full bg-white border border-gray-300 text-gray-700 rounded-lg py-2 hover:bg-gray-50 flex items-center relative"
      >
        <span className="absolute left-4 top-1/2 -translate-y-1/2">
          <Logo className="size-8" />
        </span>
        <span className="w-full text-center">
          Continue with Google
        </span>

      </button>
    </div>
  );
}
