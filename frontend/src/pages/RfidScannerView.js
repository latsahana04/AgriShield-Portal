import React, { useState } from 'react';
import apiClient from '../api/client';
import { Scan, AlertOctagon, CheckCircle2, Clock, MapPin, Scale } from 'lucide-react';

export const RfidScannerView = () => {
  const [tagInput, setTagInput] = useState('');
  const [scanResult, setScanResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const executeScan = async (tag) => {
    if (!tag) return;
    setError('');
    setLoading(true);
    try {
      const response = await apiClient.get(`/animals/scan/${tag.trim().toUpperCase()}`);
      setScanResult(response.data);
    } catch (err) {
      setScanResult(null);
      setError(err.response?.data?.message || `Animal with tag "${tag}" not found in master database.`);
    } finally {
      setLoading(false);
    }
  };

  const formatCountdown = (totalSec) => {
    if (!totalSec || totalSec <= 0) return '00h 00m 00s (CLEAR)';
    const h = Math.floor(totalSec / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const s = totalSec % 60;
    return `${String(h).padStart(2, '0')}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 pb-4 mb-5">
          <div>
            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
              <Scan className="w-5 h-5 text-emerald-600" />
              <span>Sub-Second RFID / Barcode Scanner</span>
            </h3>
            <p className="text-xs text-slate-500">Instant MRL chemical hold validation prior to milking or transport</p>
          </div>
          <span className="bg-emerald-100 text-emerald-800 text-[10px] font-mono px-2 py-1 rounded font-bold">
            UHF 915 MHz ACTIVE
          </span>
        </div>

        {/* Input Controls */}
        <div className="relative bg-slate-900 rounded-2xl p-6 text-center border-2 border-slate-800">
          <div className="text-slate-400 text-xs font-mono uppercase tracking-widest mb-3">
            [ Scan or Enter Ear Tag ID ]
          </div>
          <div className="flex max-w-sm mx-auto gap-2">
            <input
              type="text"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && executeScan(tagInput)}
              placeholder="e.g. COW-804"
              className="flex-1 bg-slate-800 border border-slate-700 text-white font-mono text-sm px-4 py-2.5 rounded-xl focus:border-emerald-400 outline-none uppercase font-bold tracking-wider text-center"
            />
            <button
              onClick={() => executeScan(tagInput)}
              disabled={loading}
              className="px-5 py-2.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold text-xs uppercase tracking-wider rounded-xl transition"
            >
              {loading ? 'Checking...' : 'Scan'}
            </button>
          </div>

          {/* Quick Demo Tags */}
          <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-center gap-2 flex-wrap text-xs text-slate-400">
            <span>Quick Test:</span>
            <button onClick={() => { setTagInput('COW-804'); executeScan('COW-804'); }} className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-amber-300 rounded font-mono text-[11px] border border-slate-700">COW-804 (Hold Active)</button>
            <button onClick={() => { setTagInput('COW-805'); executeScan('COW-805'); }} className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-emerald-300 rounded font-mono text-[11px] border border-slate-700">COW-805 (Safe Cleared)</button>
            <button onClick={() => { setTagInput('PIG-045'); executeScan('PIG-045'); }} className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-rose-300 rounded font-mono text-[11px] border border-slate-700">PIG-045 (Locked 70h)</button>
          </div>
        </div>

        {error && (
          <div className="mt-6 p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-xs text-center font-medium">
            {error}
          </div>
        )}

        {/* Dynamic SAFE or LOCKED Result Card */}
        {scanResult && (
          <div className="mt-6">
            {scanResult.status === 'LOCKED' ? (
              <div className="bg-rose-50 border-2 border-rose-500 rounded-2xl p-6 shadow-md animate-pulse">
                <div className="flex items-center justify-between pb-3 border-b border-rose-200 mb-4">
                  <div className="flex items-center gap-3">
                    <AlertOctagon className="w-8 h-8 text-rose-600" />
                    <div>
                      <h4 className="text-lg font-black text-rose-700 font-mono">{scanResult.tagId} — LOCKED</h4>
                      <span className="text-xs font-bold text-rose-800 uppercase tracking-wider">DO NOT MILK / SHIP / SLAUGHTER</span>
                    </div>
                  </div>
                  <span className="px-3 py-1 bg-rose-600 text-white font-mono text-xs font-bold rounded-lg shadow-sm">
                    HTTP 423 LOCKED
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                  <div className="bg-white p-3.5 rounded-xl border border-rose-200 space-y-1">
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Active Chemical Hold</span>
                    <strong className="text-slate-800 text-sm block">{scanResult.lastDrugAdministered}</strong>
                    <div className="text-slate-500">Latency: {scanResult.lookupLatencyMs}ms (Sub-10ms target)</div>
                  </div>
                  <div className="bg-white p-3.5 rounded-xl border border-rose-200 space-y-1">
                    <span className="text-rose-500 block text-[10px] uppercase font-bold">Remaining Hold Window</span>
                    <div className="font-mono text-xl font-black text-rose-600">
                      {formatCountdown(scanResult.remainingWithdrawalSeconds)}
                    </div>
                    <div className="text-slate-500">Release: {new Date(scanResult.safeReleaseTime).toLocaleString()}</div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="bg-emerald-50 border-2 border-emerald-500 rounded-2xl p-6 shadow-md">
                <div className="flex items-center justify-between pb-3 border-b border-emerald-200 mb-4">
                  <div className="flex items-center gap-3">
                    <CheckCircle2 className="w-8 h-8 text-emerald-600" />
                    <div>
                      <h4 className="text-lg font-black text-emerald-800 font-mono">{scanResult.tagId} — SAFE / CLEAR</h4>
                      <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider">MRL COMPLIANT — CLEAR FOR TRANSPORT</span>
                    </div>
                  </div>
                  <span className="px-3 py-1 bg-emerald-600 text-white font-mono text-xs font-bold rounded-lg shadow-sm">
                    HTTP 200 OK
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                  <div className="bg-white p-3.5 rounded-xl border border-emerald-200">
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Species & Breed</span>
                    <strong className="text-slate-800">{scanResult.species} ({scanResult.breed})</strong>
                  </div>
                  <div className="bg-white p-3.5 rounded-xl border border-emerald-200">
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Barn Location</span>
                    <strong className="text-slate-800">{scanResult.barn} · {scanResult.pen}</strong>
                  </div>
                  <div className="bg-white p-3.5 rounded-xl border border-emerald-200">
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Lookup Latency</span>
                    <strong className="text-emerald-700 font-mono">{scanResult.lookupLatencyMs} ms</strong>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
