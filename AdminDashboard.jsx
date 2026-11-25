
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/axiosConfig";
import AddEmployee from "./AddEmployee";
import HrmsChatbot from "../../components/HrmsChatbot";
import "./AdminDashboard.css";

function AdminDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState("Overview");
  const [employees, setEmployees] = useState([]);
  const [pendingLeaves, setPendingLeaves] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [selectedEmployee, setSelectedEmployee] = useState("");
  const [payrollMonth, setPayrollMonth] = useState(
    new Date().toISOString().slice(0, 7) + "-01"
  );
  const [payrollPreview, setPayrollPreview] = useState(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [showAddEmployee, setShowAddEmployee] = useState(false);

  const tabs = [
    "Overview",
    "Employees",
    "Attendance",
    "Leaves",
    "Payroll",
    "Audit Logs",
    "HRMS Assistant",
  ];

  const handleLogout = () => {
    logout();
    navigate("/", { replace: true });
  };

  const loadData = async () => {
    setLoading(true);

    const [emp, leaves, logs] = await Promise.allSettled([
      api.get("/employees"),
      api.get("/leaves/pending"),
      api.get("/audit-logs"),
    ]);

    if (emp.status === "fulfilled") {
      setEmployees(Array.isArray(emp.value.data) ? emp.value.data : []);
    }

    if (leaves.status === "fulfilled") {
      setPendingLeaves(Array.isArray(leaves.value.data) ? leaves.value.data : []);
    }

    if (logs.status === "fulfilled") {
      setAuditLogs(Array.isArray(logs.value.data) ? logs.value.data : []);
    }

    setLoading(false);
  };

  useEffect(() => {
    loadData();
  }, []);

  const loadAttendance = async (employeeId) => {
    if (!employeeId) return;

    try {
      const response = await api.get(`/attendance/today/${employeeId}`);
      const data = response.data;
      setAttendance(Array.isArray(data) ? data : data ? [data] : []);
    } catch {
      setAttendance([]);
      setMessage("No attendance record found for this employee today.");
    }
  };

  const handleLeave = async (leaveId, action) => {
    try {
      await api.put(`/leaves/${leaveId}/${action}`);
      setMessage(`Leave ${action === "approve" ? "approved" : "rejected"} successfully.`);
      loadData();
    } catch (error) {
      setMessage(error.response?.data?.message || "Unable to update leave.");
    }
  };

  const preparePayroll = async () => {
    try {
      const response = await api.post(
        `/payroll/preview?payrollMonth=${payrollMonth}`
      );
      setPayrollPreview(response.data);
      setMessage("Payroll preview generated. Review before confirmation.");
    } catch (error) {
      setMessage(error.response?.data?.message || "Unable to prepare payroll.");
    }
  };

  const confirmPayroll = async () => {
    const confirmationId =
      payrollPreview?.confirmationId || payrollPreview?.id;

    if (!confirmationId) {
      setMessage("Confirmation ID was not found.");
      return;
    }

    if (!window.confirm("Are you sure you want to confirm this payroll?")) {
      return;
    }

    try {
      await api.post(`/payroll/confirm/${confirmationId}`);
      setMessage("Payroll confirmed successfully.");
      setPayrollPreview(null);
    } catch (error) {
      setMessage(error.response?.data?.message || "Payroll confirmation failed.");
    }
  };

  const renderOverview = () => (
    <>
      <div className="hrms-welcome">
        <div>
          <h1>Dashboard Overview</h1>
          <p>Welcome back, {user?.email || "Administrator"}</p>
        </div>
        <button className="hrms-secondary-btn" onClick={loadData}>
          ↻ Refresh Data
        </button>
      </div>

      <div className="hrms-stat-grid">
        <div className="hrms-stat-card">
          <div className="hrms-stat-icon blue">♙</div>
          <div><p>Total Employees</p><h2>{employees.length}</h2></div>
        </div>

        <div className="hrms-stat-card">
          <div className="hrms-stat-icon orange">◷</div>
          <div><p>Pending Leaves</p><h2>{pendingLeaves.length}</h2></div>
        </div>

        <div className="hrms-stat-card">
          <div className="hrms-stat-icon green">▤</div>
          <div><p>Audit Records</p><h2>{auditLogs.length}</h2></div>
        </div>
      </div>

      <div className="hrms-panel hrms-quick-panel">
        <h2>Quick Actions</h2>
        <p>Choose a module to manage your workforce.</p>

        <div className="hrms-quick-actions">
          {[
            ["Employees", "＋", "Manage employee records"],
            ["Attendance", "◷", "View attendance"],
            ["Leaves", "▣", "Review leave requests"],
            ["Payroll", "₹", "Prepare payroll"],
          ].map(([tab, icon, description]) => (
            <button
              className="hrms-action-card"
              key={tab}
              onClick={() => setActiveTab(tab)}
            >
              <span>{icon}</span>
              <strong>{tab}</strong>
              <small>{description}</small>
            </button>
          ))}
        </div>
      </div>

      <div className="hrms-panel">
        <div className="hrms-panel-heading">
          <h2>Recent Employees</h2>
          <button
            className="hrms-text-btn"
            onClick={() => setActiveTab("Employees")}
          >
            View all →
          </button>
        </div>
        <EmployeeTable employees={employees.slice(0, 5)} />
      </div>
    </>
  );

  const renderEmployees = () => (
    <section className="hrms-panel">
      <div className="hrms-panel-heading">
        <div>
          <h2>Employee Management</h2>
          <p>View your registered employees.</p>
        </div>
        <div className="hrms-employee-actions">
          <span className="hrms-count">{employees.length} employees</span>
          <button
            type="button"
            className="hrms-secondary-btn"
            onClick={() => setShowAddEmployee((visible) => !visible)}
          >
            {showAddEmployee ? "Close form" : "+ Add Employee"}
          </button>
        </div>
      </div>
      {showAddEmployee && (
        <AddEmployee onEmployeeCreated={() => {
          loadData();
        }} />
      )}
      <EmployeeTable employees={employees} />
    </section>
  );

  const renderAttendance = () => (
    <section className="hrms-panel">
      <h2>Attendance Management</h2>
      <p>Choose an employee to view today's attendance.</p>

      <div className="hrms-form-row">
        <select
          value={selectedEmployee}
          onChange={(e) => {
            setSelectedEmployee(e.target.value);
            loadAttendance(e.target.value);
          }}
        >
          <option value="">Select Employee</option>
          {employees.map((employee) => (
            <option key={employee.id} value={employee.id}>
              {employee.fullName || employee.name || "Employee"}
            </option>
          ))}
        </select>
      </div>

      {attendance.length > 0 ? (
        <div className="hrms-table-wrap">
          <table>
            <thead>
              <tr>
                <th>Employee</th><th>Date</th><th>Punch In</th>
                <th>Punch Out</th><th>Status</th>
              </tr>
            </thead>
            <tbody>
              {attendance.map((item, index) => (
                <tr key={item.id || index}>
                  <td>{item.employeeName || "Selected employee"}</td>
                  <td>{item.date || "—"}</td>
                  <td>{item.punchIn || item.punchInTime || "—"}</td>
                  <td>{item.punchOut || item.punchOutTime || "—"}</td>
                  <td><span className="hrms-status">{item.status || "Recorded"}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="hrms-empty">Select an employee to view attendance records.</div>
      )}
    </section>
  );

  const renderLeaves = () => (
    <section className="hrms-panel">
      <div className="hrms-panel-heading">
        <div>
          <h2>Leave Requests</h2>
          <p>Review and manage pending leave applications.</p>
        </div>
        <span className="hrms-count">{pendingLeaves.length} pending</span>
      </div>

      {pendingLeaves.length === 0 ? (
        <div className="hrms-empty">There are no pending leave requests.</div>
      ) : (
        <div className="hrms-table-wrap">
          <table>
            <thead>
              <tr>
                <th>Employee</th><th>Leave Type</th><th>From</th>
                <th>To</th><th>Reason</th><th>Action</th>
              </tr>
            </thead>
            <tbody>
              {pendingLeaves.map((leave) => (
                <tr key={leave.id}>
                  <td>{leave.employeeName || leave.employee?.fullName || leave.employeeId || "—"}</td>
                  <td>{leave.leaveType || "—"}</td>
                  <td>{leave.startDate || "—"}</td>
                  <td>{leave.endDate || "—"}</td>
                  <td>{leave.reason || "—"}</td>
                  <td>
                    <div className="hrms-inline-actions">
                      <button className="hrms-approve-btn" onClick={() => handleLeave(leave.id, "approve")}>Approve</button>
                      <button className="hrms-reject-btn" onClick={() => handleLeave(leave.id, "reject")}>Reject</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );

  const renderPayroll = () => (
    <section className="hrms-panel">
      <h2>Payroll Management</h2>
      <p>Generate a payroll preview and review it before confirming.</p>

      <div className="hrms-form-row">
        <label htmlFor="payroll-month">Payroll month</label>
        <input
          id="payroll-month"
          type="month"
          value={payrollMonth.slice(0, 7)}
          onChange={(e) => setPayrollMonth(`${e.target.value}-01`)}
        />
        <button className="hrms-primary-btn" onClick={preparePayroll}>
          Generate Preview
        </button>
      </div>

      {payrollPreview && (
        <div className="hrms-preview-box">
          <h3>Payroll Preview</h3>
          <pre>{JSON.stringify(payrollPreview, null, 2)}</pre>
          <button className="hrms-primary-btn" onClick={confirmPayroll}>
            Confirm Payroll
          </button>
        </div>
      )}
    </section>
  );

  const renderAuditLogs = () => (
    <section className="hrms-panel">
      <div className="hrms-panel-heading">
        <div>
          <h2>Audit Logs</h2>
          <p>Recent recorded system activities.</p>
        </div>
        <span className="hrms-count">{auditLogs.length} records</span>
      </div>

      {auditLogs.length === 0 ? (
        <div className="hrms-empty">No audit records found.</div>
      ) : (
        <div className="hrms-table-wrap">
          <table>
            <thead>
              <tr>
                <th>User</th><th>Action</th><th>Entity</th>
                <th>Details</th><th>Date</th>
              </tr>
            </thead>
            <tbody>
              {auditLogs.map((log) => (
                <tr key={log.id}>
                  <td>{log.userEmail || log.userId || "—"}</td>
                  <td>{log.action || "—"}</td>
                  <td>{log.entityType || "—"}</td>
                  <td>{log.details || "—"}</td>
                  <td>{log.createdAt ? new Date(log.createdAt).toLocaleString() : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );

  return (
    <div className="hrms-admin-layout">
      <aside className="hrms-sidebar">
        <div className="hrms-brand">
          <div className="hrms-brand-mark">iC</div>
          <div>
            <h2>iCeipt HRMS</h2>
            <p>Admin Portal</p>
          </div>
        </div>

        <div className="hrms-nav-label">WORKSPACE</div>

        <nav className="hrms-navigation">
          {tabs.map((tab, index) => (
            <button
              key={tab}
              className={activeTab === tab ? "active" : ""}
              onClick={() => setActiveTab(tab)}
            >
              <span>{["⌂", "♙", "◷", "▣", "₹", "▤", "✦"][index]}</span>
              {tab}
            </button>
          ))}
        </nav>

        <div className="hrms-sidebar-bottom">
          <div className="hrms-admin-profile">
            <div className="hrms-avatar">A</div>
            <div>
              <strong>Administrator</strong>
              <small>{user?.email || "Admin account"}</small>
            </div>
          </div>

          <button className="hrms-logout-btn" onClick={handleLogout}>
            ↪ Log out
          </button>
        </div>
      </aside>

      <main className="hrms-main">
        <header className="hrms-topbar">
          <div>
            <span className="hrms-topbar-label">iCeipt HRMS / Admin</span>
            <h2>{activeTab}</h2>
          </div>
          <div className="hrms-topbar-right">
            <span className="hrms-online-dot"></span>
            <span>System Online</span>
          </div>
        </header>

        <div className="hrms-content">
          {message && (
            <div className="hrms-message">
              <span>{message}</span>
              <button onClick={() => setMessage("")}>×</button>
            </div>
          )}

          {loading && <p className="hrms-loading">Loading dashboard data...</p>}

          {activeTab === "Overview" && renderOverview()}
          {activeTab === "Employees" && renderEmployees()}
          {activeTab === "Attendance" && renderAttendance()}
          {activeTab === "Leaves" && renderLeaves()}
          {activeTab === "Payroll" && renderPayroll()}
          {activeTab === "Audit Logs" && renderAuditLogs()}
          {activeTab === "HRMS Assistant" && <HrmsChatbot />}
        </div>
      </main>
    </div>
  );
}


function EmployeeTable({ employees = [] }) {
  if (!employees.length) {
    return (
      <div className="hrms-empty">
        No employee records found.
      </div>
    );
  }

  return (
    <div className="hrms-table-wrap">
      <table>
        <thead>
          <tr>
            <th>Employee</th>
            <th>Employee Code</th>
            <th>Email</th>
            <th>Department</th>
            <th>Designation</th>
            <th>Status</th>
          </tr>
        </thead>

        <tbody>
          {employees.map((employee) => (
            <tr key={employee.id}>
              <td>
                <strong>
                  {employee.fullName || employee.name || "—"}
                </strong>
              </td>
              <td>{employee.employeeCode || "—"}</td>
              <td>{employee.email || "—"}</td>
              <td>{employee.department || "—"}</td>
              <td>{employee.designation || "—"}</td>
              <td>
                <span className="hrms-status">
                  {employee.active === false ? "Inactive" : "Active"}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default AdminDashboard;
