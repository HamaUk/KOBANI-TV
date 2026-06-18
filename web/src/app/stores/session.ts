import { create } from "zustand";
import { persist } from "zustand/middleware";
import { ref, get } from "firebase/database";
import { rtdb } from "@data/firebase";

interface SessionState {
  code: string | null;
  isLoading: boolean;
  error: string | null;
  login: (code: string) => Promise<boolean>;
  logout: () => void;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      code: null,
      isLoading: false,
      error: null,
      login: async (code: string) => {
        set({ isLoading: true, error: null });
        try {
          const normalized = code.trim().toLowerCase();
          if (!normalized) {
            set({ isLoading: false, error: "Code cannot be empty" });
            return false;
          }

          const snapshot = await get(ref(rtdb, 'sync/global/loginCodes'));
          if (snapshot.exists()) {
            const data = snapshot.val();
            const values = Array.isArray(data) ? data : Object.values(data);
            
            for (const v of values) {
              if (typeof v === 'object' && v !== null) {
                const item = v as any;
                const active = item.active !== false;
                const itemCode = String(item.code || '').trim().toLowerCase();
                
                if (active && itemCode === normalized) {
                  // Check expiry
                  if (item.expiresAt) {
                    const expiry = new Date(item.expiresAt);
                    if (new Date() > expiry) {
                      continue; // Expired
                    }
                  }
                  
                  // Valid!
                  set({ code: normalized, isLoading: false, error: null });
                  return true;
                }
              }
            }
          }
          
          set({ isLoading: false, error: "Invalid or expired code" });
          return false;
        } catch (e: any) {
          set({ isLoading: false, error: e.message || "Failed to validate code" });
          return false;
        }
      },
      logout: () => {
        set({ code: null, error: null });
      },
    }),
    {
      name: "optic-session",
    }
  )
);
