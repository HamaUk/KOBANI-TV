import { useState } from "react";
import { useSessionStore } from "@app/stores/session";
import { useI18n } from "@app/i18n";
import { syncFirebasePlaylist } from "@data/sync/firebaseSync";
import { useProviderStore } from "@app/stores/providers";

export function LoginScreen() {
  const [code, setCode] = useState("");
  const { login, isLoading, error } = useSessionStore();
  const setActiveProvider = useProviderStore(s => s.setActive);
  const t = useI18n((s) => s.t);

  const onKeyPress = (val: string) => {
    if (isLoading) return;
    if (val === "DEL") {
      setCode((prev) => prev.slice(0, -1));
    } else {
      if (code.length < 12) setCode((prev) => prev + val);
    }
  };

  const onSubmit = async () => {
    if (!code) return;
    const success = await login(code);
    if (success) {
      // Sync playlist right after login
      await syncFirebasePlaylist();
      setActiveProvider(1);
    }
  };

  return (
    <div style={{
      position: "fixed", inset: 0, backgroundColor: "#000",
      display: "flex", flexDirection: "column",
      alignItems: "center", justifyContent: "center",
      backgroundImage: "radial-gradient(circle at 50% 50%, #1A2A4E 0%, #0A101E 50%, #000 100%)",
      color: "#fff", fontFamily: "sans-serif"
    }}>
      <div style={{
        maxWidth: 400, width: "100%", padding: "40px 32px",
        background: "rgba(255,255,255,0.06)",
        backdropFilter: "blur(25px)",
        borderRadius: 32,
        border: "1px solid rgba(255,255,255,0.12)",
        boxShadow: "0 20px 40px rgba(0,0,0,0.5)",
        textAlign: "center"
      }}>
        <h1 style={{ fontSize: 24, fontWeight: 900, letterSpacing: 2, marginBottom: 8, textTransform: "uppercase" }}>
          Optic TV
        </h1>
        <p style={{ color: "rgba(255,255,255,0.5)", fontSize: 14, marginBottom: 40 }}>
          Enter your access code
        </p>

        <div style={{
          height: 80,
          background: "rgba(0,0,0,0.3)",
          borderRadius: 24,
          display: "flex", alignItems: "center", justifyContent: "center",
          marginBottom: 32,
          boxShadow: "inset 0 4px 10px rgba(0,0,0,0.5)"
        }}>
          {code ? (
            <div style={{ display: "flex", gap: 8 }}>
              {Array.from(code).map((_, i) => (
                <div key={i} style={{ width: 16, height: 16, borderRadius: 8, backgroundColor: "#fff" }} />
              ))}
            </div>
          ) : (
            <span style={{ color: "rgba(255,255,255,0.2)", fontSize: 16, fontWeight: 900, letterSpacing: 4 }}>
              ENTER CODE
            </span>
          )}
        </div>

        <div style={{
          display: "grid", gridTemplateColumns: "repeat(3, 1fr)",
          gap: 16, marginBottom: 32
        }}>
          {[1, 2, 3, 4, 5, 6, 7, 8, 9, "DEL", 0, "OK"].map((btn) => (
            <button
              key={btn}
              onClick={() => btn === "OK" ? onSubmit() : onKeyPress(String(btn))}
              disabled={isLoading}
              style={{
                height: 60,
                borderRadius: 16,
                border: btn === "OK" ? "1px solid rgba(212,175,55,0.5)" : "1px solid rgba(255,255,255,0.1)",
                background: btn === "OK" ? "rgba(212,175,55,0.2)" : "rgba(255,255,255,0.05)",
                color: btn === "OK" ? "#d4af37" : "#fff",
                fontSize: 20, fontWeight: "bold",
                cursor: "pointer",
                transition: "all 0.2s"
              }}
            >
              {btn}
            </button>
          ))}
        </div>

        {error && (
          <div style={{ color: "#ff4d4d", fontSize: 13, fontWeight: "bold", background: "rgba(255,77,77,0.1)", padding: 12, borderRadius: 12 }}>
            {error}
          </div>
        )}
      </div>
      
      {isLoading && (
        <div style={{
          position: "absolute", inset: 0, backgroundColor: "rgba(0,0,0,0.5)",
          display: "flex", alignItems: "center", justifyContent: "center"
        }}>
          <div style={{ width: 40, height: 40, borderRadius: 20, border: "4px solid rgba(212,175,55,0.3)", borderTopColor: "#d4af37", animation: "spin 1s linear infinite" }} />
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      )}
    </div>
  );
}
