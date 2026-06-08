import { Hono } from "hono";
import { AuthController } from "../controllers/auth-controller.js";
import { AuthService } from "../services/auth-service.js";
import { FirebaseUserRepository } from "../repositories/firebase-user-repository.js";
import { authMiddleware } from "../config/auth-middleware.js";

const authRouter = new Hono();

// Manual Dependency Injection / Assembly
const userRepository = new FirebaseUserRepository();
const authService = new AuthService(userRepository);
const authController = new AuthController(authService);

authRouter.post("/register", authController.register);
authRouter.post("/login", authController.login);
authRouter.get("/check-username/:username", authController.checkUsername);

// Authenticated profile routes
authRouter.put("/profile", authMiddleware, authController.updateAvatar);
authRouter.get("/profile", authMiddleware, authController.getProfile);

authRouter.put("/profile", authMiddleware, authController.updateProfile);
export { authRouter };
