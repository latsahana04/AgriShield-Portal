import React, { useState } from 'react';
import apiClient from '../api/client';
import { UploadCloud, CheckCircle, AlertTriangle, XCircle, ShieldCheck } from 'lucide-react';

export const DataCleanerConsole = () => {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [committed, setCommitted] = useState(false);
  const [toastMsg, setToastMsg] = useState('');

  const sampleCsvRows = [
    { animalTagId: 'COW-311', medicationName: 'Penicillin G Procaine', dosage: '12 ml', dosageMg: 1200, route: 'Intramuscular', vetLicenseId: 'VET-USA-4402' },
    { animalTagId: 'COW-312', medicationName: 'Ceftiofur Sodium', dosage: '10 ml', dosageMg: 1000, route: 'Subcutaneous', vetLicenseId: 'VET-USA-4402' },
    // Duplicate entry (identical)
    { animalTagId: 'COW-311', medicationName: 'Penicillin G Procaine', dosage: '12 ml', dosageMg: 1200, route: 'Intramuscular', vetLicenseId: 'VET-USA-4402' },
    // Off-duty vet anomaly
    { animalTagId: 'PIG-045', medicationName: 'Enrofloxacin', dosage: '5 ml', dosageMg: 500, route: 'Oral', vetLicenseId: 'UNKNOWN-VET-999' }
  ];

  const handleProcessIngestion = async (commitToLedger = false) => {
    setLoading(true);
    try {
      const response = await apiClient.post('/data/clean', {
        rows: sampleCsvRows,
        commitCleanToLedger: commitToLedger
      });
      setMetrics(response.data);
      if (commitToLedger) {
        setCommitted(true);
        setToastMsg('Committed clean treatment records to PostgreSQL and Hyperledger Fabric.');
      } else {
        setToastMsg('Data cleaner pipeline completed analysis.');
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-bold text-slate-900 text-base">Bulk Dataset Ingestion & Shift Duty Cleaner</h3>
          <p className="text-xs text-slate-500">Automated Two-Pass Pipeline: SHA-256 Deduplication & Vet Duty Roster Check</p>
        </div>
      </div>

      {toastMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-300 text-emerald-900 rounded-xl text-xs font-semibold flex items-center gap-2">
          <CheckCircle className="w-4 h-4 text-emerald-600" />
          <span>{toastMsg}</span>
        </div>
      )}

      {/* Drag & Drop Box */}
      <div
        onClick={() => handleProcessIngestion(false)}
        className="bg-white rounded-2xl border-2 border-dashed border-slate-300 p-8 text-center hover:border-forest transition cursor-pointer"
      >
        <UploadCloud className="w-12 h-12 text-forest mx-auto mb-2" />
        <h4 className="font-bold text-slate-900 text-sm">Upload Batch CSV / Excel Prescription Sheet</h4>
        <p className="text-xs text-slate-500 mt-1">Click to analyze sample batch through the two-pass pipeline</p>
      </div>

      {/* Real-time Metric Status Cards */}
      {metrics && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm text-center">
              <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">Total Ingested</span>
              <strong className="text-2xl font-black text-slate-900 mt-1 block">{metrics.totalProcessed}</strong>
            </div>

            <div className="bg-white p-4 rounded-xl border border-emerald-300 shadow-sm text-center">
              <span className="text-[10px] text-emerald-700 uppercase font-bold tracking-wider block">Approved Clean</span>
              <strong className="text-2xl font-black text-emerald-600 mt-1 block">{metrics.approvedCleanCount}</strong>
              <span className="text-[10px] text-emerald-600 font-medium">Ready for MRL Engine</span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-amber-300 shadow-sm text-center">
              <span className="text-[10px] text-amber-700 uppercase font-bold tracking-wider block">Rejected Duplicates</span>
              <strong className="text-2xl font-black text-amber-600 mt-1 block">{metrics.rejectedDuplicatesCount}</strong>
              <span className="text-[10px] text-amber-600 font-medium">Pass 1 Hash Match</span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-rose-300 shadow-sm text-center">
              <span className="text-[10px] text-rose-700 uppercase font-bold tracking-wider block">Shift Anomalies</span>
              <strong className="text-2xl font-black text-rose-600 mt-1 block">{metrics.flaggedAnomaliesCount}</strong>
              <span className="text-[10px] text-rose-600 font-medium">Pass 2 Off-Duty</span>
            </div>
          </div>

          {/* Action Bar */}
          <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex items-center justify-between">
            <div>
              <h4 className="font-bold text-slate-900 text-sm">Commit Clean Records to Blockchain Ledger</h4>
              <p className="text-xs text-slate-500">
                {metrics.approvedCleanCount > 0
                  ? `${metrics.approvedCleanCount} verified records will update animal MRL states to LOCKED.`
                  : 'Zero clean records to commit.'}
              </p>
            </div>
            <button
              onClick={() => handleProcessIngestion(true)}
              disabled={loading || committed || metrics.approvedCleanCount === 0}
              className="px-5 py-2.5 bg-forest hover:bg-forest-light text-white font-bold rounded-xl text-xs uppercase tracking-wider transition shadow-sm disabled:opacity-50"
            >
              {committed ? '✅ Committed to Fabric' : '🔐 Commit Clean Records'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
