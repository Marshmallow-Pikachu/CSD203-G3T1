const GOOGLE_AUTH_URL = "http://localhost:8080/oauth2/authorization/google";

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