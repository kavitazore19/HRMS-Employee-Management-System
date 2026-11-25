# HRMS – Human Resource Management System

## Project Overview

HRMS (Human Resource Management System) is a full-stack web application designed to simplify and manage organizational human resource activities.

The application provides separate Admin and Employee portals to manage employee information and HR-related operations through a centralized platform.

## Technologies Used

**Frontend**
- React.js
- JavaScript
- HTML5
- CSS3
- Vite

**Backend**
- Java
- Spring Boot
- Spring Security
- REST APIs
- Maven

**Database**
- MySQL

**AI Integration**
- Google Gemini API

## Key Features

### Admin Portal
- Admin login and authentication
- Admin dashboard
- Employee management
- Employee records and information
- Attendance management
- Leave management
- Payroll management
- HRMS AI chatbot

### Employee Portal
- Employee login
- Employee dashboard
- View employee information
- Access attendance details
- Access leave information
- View payroll information
- Logout functionality

### AI Chatbot
- Integrated AI-powered chatbot using Google Gemini
- Assists users with HR-related queries

## Project Structure

```text
HRMS-Employee-Management-System/
│
├── hrms-frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── index.html
│
├── hrms-backend/
│   ├── src/
│   │   └── main/
│   └── pom.xml
│
├── README.md
└── .gitignore
```

## Installation and Setup

### 1. Clone the Repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

### 2. Backend Setup

```bash
cd hrms-backend
mvn spring-boot:run
```

### 3. Frontend Setup

Open another terminal:

```bash
cd hrms-frontend
npm install
npm run dev
```

### 4. Database Configuration

Configure your MySQL database connection in the backend application properties using your local database credentials.

### 5. Gemini API Configuration

Configure your Gemini API key securely using an environment variable or another private configuration method.

Do not publish API keys or database passwords in the repository.

## Project Objective

The main objective of this project is to develop a centralized HR management platform that simplifies employee administration, improves accessibility to HR services, and provides an AI-powered chatbot for assistance.

## Author

**Kavita Zore**

GitHub: https://github.com/kavitazore19

---

**Project:** Human Resource Management System (HRMS)
