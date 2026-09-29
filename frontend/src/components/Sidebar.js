import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Scan, Database, Truck, FileCheck2, ShieldAlert, BarChart3, Users, Calendar } from 'lucide-react';

export const Sidebar = () => {
  const { user } = useAuth();
  const role = user ? user.role : null;

  const links = [];

  // Farmhand & Manager
  if (role === 'FARMHAND' || role === 'MANAGER') {
    links.push({
      category: 'Farmhand Tools',
      items: [
        { to: '/farmhand/scan', label: 'RFID Tag Scanner', icon: Scan },
      ]
    });
  }

  // Veterinarian & Manager
  if (role === 'VETERINARIAN' || role === 'MANAGER') {
    links.push({
      category: 'Veterinary Hub',
      items: [
        { to: '/vet/data-cleaner', label: 'Bulk Data Cleaner', icon: Database },
      ]
    });
  }

  // Manager
  if (role === 'MANAGER') {
    links.push({
      category: 'Logistics Gatekeeper',
      items: [
        { to: '/manager/shipments/request-permit', label: 'Shipment Gatekeeper', icon: Truck },
      ]
    });
  }

  // Auditor & Manager
  if (role === 'AUDITOR' || role === 'MANAGER') {
    links.push({
      category: 'Regulatory Audit',
      items: [
        { to: '/auditor/blockchain-explorer', label: 'Ledger Explorer', icon: FileCheck2 },
      ]
    });
  }

  return (
    <aside className="w-64 bg-forest text-slate-200 flex flex-col border-r border-brand-950 flex-shrink-0">
      <div className="p-4 border-b border-white/10 flex items-center gap-2.5">
        <div className="w-8 h-8 rounded-lg bg-amber-custom flex items-center justify-center text-white font-black text-sm">
          🛡️
        </div>
        <div>
          <h1 className="font-extrabold text-sm tracking-tight text-white leading-none">AGRISHIELD</h1>
          <span className="text-[10px] text-emerald-300 font-mono">PORTAL V2.5</span>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto p-3 space-y-4 text-xs">
        {links.map((sec, idx) => (
          <div key={idx}>
            <div className="px-2 mb-1.5 text-[10px] font-bold uppercase tracking-wider text-emerald-400/80">
              {sec.category}
            </div>
            <div className="space-y-0.5">
              {sec.items.map((item, i) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={i}
                    to={item.to}
                    className={({ isActive }) =>
                      `flex items-center gap-2.5 px-3 py-2 rounded-lg transition font-medium ${
                        isActive
                          ? 'bg-emerald-500/20 text-white font-semibold border-l-4 border-emerald-400'
                          : 'text-slate-300 hover:bg-white/5 hover:text-white'
                      }`
                    }
                  >
                    <Icon className="w-4 h-4 text-emerald-300" />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="p-3 bg-brand-950/80 border-t border-white/10 text-[11px] text-slate-400 font-mono">
        Active Node: Org1 (Farm)
      </div>
    </aside>
  );
};
