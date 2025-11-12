// components/buttons/SocialAuthButtons.tsx
const API_BASE =
  import.meta.env.VITE_API_BASE ||
  window.location.origin; // fallback to same origin if env var missing

const GOOGLE_AUTH_URL = `${API_BASE}/oauth2/authorization/google`;

const go = (url: string) => {
  window.location.href = url;
};

export default function SocialAuthButtons() {
  return (
    <div className="space-y-2">
      <button
        type="button"
        onClick={() => go(GOOGLE_AUTH_URL)}
        className="w-full bg-white border border-gray-300 text-gray-700 rounded-lg py-2 hover:bg-gray-50"
      >
        Continue with Google
      </button>
    </div>
  );
}
