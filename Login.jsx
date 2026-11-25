
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { LockKeyhole, Mail, Users } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import "./Login.css";

function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            const user = await login(email, password);

            if (user.role === "ADMIN") {
                navigate("/admin");
            } else if (user.role === "EMPLOYEE") {
                navigate("/employee");
            } else {
                setError("Invalid user role.");
            }
        } catch (err) {
            console.error("Login error:", err.response?.status, err.response?.data);
            setError(
                err.response?.data?.message ||
                "Login failed. Please check your credentials."
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <div className="login-card">

                <div className="login-icon">
                    <Users size={30} />
                </div>

                <h1>iCeipt HRMS</h1>

                <p className="login-subtitle">
                    Human Resource Management System
                </p>

                <h2>Welcome Back</h2>

                <p className="login-description">
                    Sign in to access your account
                </p>

                <form onSubmit={handleSubmit}>

                    <label>Email Address</label>

                    <div className="login-input">
                        <Mail size={18} />

                        <input
                            type="email"
                            placeholder="Enter your email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    <label>Password</label>

                    <div className="login-input password-field">
                        <LockKeyhole size={18} />

                        <input
                            type={showPassword ? "text" : "password"}
                            placeholder="Enter your password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />

                        <button
                            type="button"
                            className="password-toggle"
                            onClick={() =>
                                setShowPassword((previous) => !previous)
                            }
                        >
                            {showPassword ? "Hide" : "Show"}
                        </button>
                    </div>

                    {error && (
                        <p className="login-error">{error}</p>
                    )}

                    <button
                        type="submit"
                        className="login-submit"
                        disabled={loading}
                    >
                        {loading ? "Signing in..." : "Sign In"}
                    </button>

                </form>

                <p className="login-footer">
                    Secure Employee & Administrator Portal
                </p>

            </div>
        </div>
    );
}

export default Login;
