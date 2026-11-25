
import { useState } from "react";
import api from "../../api/axiosConfig";
import "./AddEmployee.css";

function AddEmployee({ onEmployeeCreated }) {
  const [form, setForm] = useState({
    employeeCode: "",
    fullName: "",
    email: "",
    department: "",
    designation: "",
    joiningDate: "",
    salary: "",
    temporaryPassword: "",
  });

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");
    setError("");

    if (form.temporaryPassword.length < 8) {
      setError("Password must contain at least 8 characters.");
      return;
    }

    try {
      setLoading(true);

      await api.post("/employees", {
        ...form,
        salary: Number(form.salary),
      });

      setMessage("Employee created successfully!");

      setForm({
        employeeCode: "",
        fullName: "",
        email: "",
        department: "",
        designation: "",
        joiningDate: "",
        salary: "",
        temporaryPassword: "",
      });

      if (onEmployeeCreated) {
        onEmployeeCreated();
      }
    } catch (err) {
      const responseData = err.response?.data;
      const validationMessages = responseData?.errors
        ? Object.values(responseData.errors).join(" ")
        : "";
      const backendMessage = typeof responseData === "string"
        ? responseData
        : responseData?.message || validationMessages;
      setError(backendMessage || "Unable to create employee. Please check the details.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="add-employee-container">
      <div className="add-employee-heading">
        <div>
          <h2>Add New Employee</h2>
          <p>Create an employee record and login account.</p>
        </div>
      </div>

      <form className="add-employee-form" onSubmit={handleSubmit}>
        <div className="employee-form-grid">
          <div className="employee-field">
            <label>Employee Code *</label>
            <input
              name="employeeCode"
              value={form.employeeCode}
              onChange={handleChange}
              placeholder="EMP003"
              required
            />
          </div>

          <div className="employee-field">
            <label>Full Name *</label>
            <input
              name="fullName"
              value={form.fullName}
              onChange={handleChange}
              placeholder="Enter full name"
              required
            />
          </div>

          <div className="employee-field">
            <label>Email Address *</label>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="employee@example.com"
              required
            />
          </div>

          <div className="employee-field">
            <label>Department</label>
            <input
              name="department"
              value={form.department}
              onChange={handleChange}
              placeholder="IT, HR, Finance..."
            />
          </div>

          <div className="employee-field">
            <label>Designation</label>
            <input
              name="designation"
              value={form.designation}
              onChange={handleChange}
              placeholder="Software Developer"
            />
          </div>

          <div className="employee-field">
            <label>Joining Date *</label>
            <input
              type="date"
              name="joiningDate"
              value={form.joiningDate}
              onChange={handleChange}
              required
            />
          </div>

          <div className="employee-field">
            <label>Monthly Salary *</label>
            <input
              type="number"
              name="salary"
              value={form.salary}
              onChange={handleChange}
              placeholder="25000"
              min="1"
              step="0.01"
              required
            />
          </div>

          <div className="employee-field">
            <label>Temporary Password *</label>
            <input
              type="password"
              name="temporaryPassword"
              value={form.temporaryPassword}
              onChange={handleChange}
              placeholder="Minimum 8 characters"
              minLength="8"
              required
            />
          </div>
        </div>

        {message && <div className="employee-success">{message}</div>}
        {error && <div className="employee-error">{error}</div>}

        <button
          type="submit"
          className="employee-submit-button"
          disabled={loading}
        >
          {loading ? "Creating..." : "Create Employee"}
        </button>
      </form>
    </div>
  );
}

export default AddEmployee;
