import { useState } from "react";
import api from "../api/axiosConfig";
import "./HrmsChatbot.css";

function HrmsChatbot() {
  const [messages, setMessages] = useState([
    { role: "assistant", text: "Hi! I can help with HRMS attendance, leave, and payroll tasks available to your account." },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const sendMessage = async (event) => {
    event.preventDefault();
    const message = input.trim();
    if (!message || loading) return;

    setMessages((current) => [...current, { role: "user", text: message }]);
    setInput("");
    setLoading(true);

    try {
      const response = await api.post("/chatbot/chat", { message });
      setMessages((current) => [...current, {
        role: "assistant",
        text: response.data?.reply || "I received an empty response. Please try again.",
      }]);
    } catch (error) {
      const detail = error.response?.data?.reply
        || error.response?.data?.message
        || (typeof error.response?.data === "string" ? error.response.data : null)
        || "The assistant is unavailable right now. Please try again.";
      setMessages((current) => [...current, { role: "assistant error", text: detail }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="hrms-chatbot" aria-label="HRMS assistant">
      <header className="hrms-chatbot-header">
        <div>
          <h2>HRMS Assistant</h2>
          <p>Ask for help with your HRMS tasks.</p>
        </div>
      </header>
      <div className="hrms-chatbot-messages" aria-live="polite">
        {messages.map((message, index) => (
          <div className={`hrms-chat-message ${message.role.replace(" ", "-")}`} key={`${index}-${message.role}`}>
            <span>{message.role === "user" ? "You" : "Assistant"}</span>
            <p>{message.text}</p>
          </div>
        ))}
        {loading && <p className="hrms-chatbot-loading">Assistant is thinking…</p>}
      </div>
      <form className="hrms-chatbot-form" onSubmit={sendMessage}>
        <label className="sr-only" htmlFor="hrms-chat-input">Message</label>
        <input
          id="hrms-chat-input"
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="For example: check my leave balance"
          maxLength={2000}
          disabled={loading}
        />
        <button type="submit" disabled={loading || !input.trim()}>
          {loading ? "Sending…" : "Send"}
        </button>
      </form>
    </section>
  );
}

export default HrmsChatbot;
