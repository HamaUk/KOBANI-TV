import { initializeApp } from "firebase/app";
import { getDatabase } from "firebase/database";

const firebaseConfig = {
  apiKey: "AIzaSyCvNllsitniHSvzTIKiH74EqgCrHqB5xJI",
  databaseURL: "https://optic-tv-default-rtdb.firebaseio.com",
  projectId: "optic-tv",
  storageBucket: "optic-tv.firebasestorage.app",
};

export const app = initializeApp(firebaseConfig);
export const rtdb = getDatabase(app);
