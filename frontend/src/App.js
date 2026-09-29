import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { ProtectedRoute } from './components/ProtectedRoute';

import { Login } from './pages/Login';
import { RfidScannerView } from './pages/RfidScannerView';
import { DataCleanerConsole } from './pages/DataCleanerConsole';
import { ShipmentGatekeeperView } from './pages/ShipmentGatekeeperView';
import { AuditorExplorerView } from './pages/AuditorExplorerView';

const Layout = ({ children }) => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="h-screen flex flex-col overflow-hidden bg-slate-50">
      <Navbar />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Route */}
          <Route path="/login" element={<Login />} />

          {/* Unauthorized View */}
          <Route
            path="/unauthorized"
            element={
              <div className="h-screen flex items-center justify-center text-center p-6 bg-slate-50">
                <div className="max-w-md bg-white p-8 rounded-2xl border border-rose-200 shadow-sm">
                  <h2 className="text-xl font-bold text-rose-700">HTTP 403 Forbidden</h2>
                  <p className="text-xs text-slate-600 mt-2">
                    Your current role does not have authorization to access this view.
                  </p>
                  <a href="/login" className="mt-4 inline-block px-4 py-2 bg-slate-800 text-white text-xs font-semibold rounded-lg">
                    Return to Login
                  </a>
                </div>
              </div>
            }
          />

          {/* Farmhand Routes */}
          <Route
            path="/farmhand/scan"
            element={
              <ProtectedRoute allowedRoles={['FARMHAND', 'MANAGER']}>
                <Layout>
                  <RfidScannerView />
                </Layout>
              </ProtectedRoute>
            }
          />

          {/* Vet Routes */}
          <Route
            path="/vet/data-cleaner"
            element={
              <ProtectedRoute allowedRoles={['VETERINARIAN', 'MANAGER']}>
                <Layout>
                  <DataCleanerConsole />
                </Layout>
              </ProtectedRoute>
            }
          />

          {/* Manager Routes */}
          <Route
            path="/manager/shipments/request-permit"
            element={
              <ProtectedRoute allowedRoles={['MANAGER']}>
                <Layout>
                  <ShipmentGatekeeperView />
                </Layout>
              </ProtectedRoute>
            }
          />

          {/* Auditor Routes */}
          <Route
            path="/auditor/blockchain-explorer"
            element={
              <ProtectedRoute allowedRoles={['AUDITOR', 'MANAGER']}>
                <Layout>
                  <AuditorExplorerView />
                </Layout>
              </ProtectedRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}
