import React, { useState } from 'react';
import apiClient from '../api/client';
import { Truck, AlertOctagon, CheckCircle2, FileText, Send } from 'lucide-react';

export const ShipmentGatekeeperView = () => {
  // Preset demo herd batch
  const initialHerd = [
    { tagId: 'COW-311', species: 'Bovine', barn: 'Barn 3', status: 'SAFE' },
    { tagId: 'COW-312', species: 'Bovine', barn: 'Barn 3', status: 'SAFE' },
    { tagId: 'HEN-222', species: 'Poultry', barn: 'Barn 4', status: 'SAFE' },
    { tagId: 'COW-804', species: 'Bovine', barn: 'Barn 1', status: 'LOCKED', drug: 'Penicillin G Procaine (Hold active)' },
    { tagId: 'PIG-045', species: 'Swine', barn: 'Barn 2', status: 'LOCKED', drug: 'Penicillin G Procaine' }
  ];

  const [selectedTags, setSelectedTags] = useState(['COW-311', 'COW-312', 'HEN-222']);
  const [permitResult, setPermitResult] = useState(null);
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  // Check if any selected tag is locked
  const lockedInBatch = initialHerd.filter(a => selectedTags.includes(a.tagId) && a.status === 'LOCKED');
  const isBatchCompliant = selectedTags.length > 0 && lockedInBatch.length === 0;

  const toggleTag = (tagId) => {
    setSelectedTags(prev =>
      prev.includes(tagId) ? prev.filter(t => t !== tagId) : [...prev, tagId]
    );
    setPermitResult(null);
    setErrorMsg('');
  };

  const handleRequestPermit = async () => {
    setLoading(true);
    setErrorMsg('');
    setPermitResult(null);

    try {
      const response = await apiClient.post('/shipments/permit', {
        animalTagIds: selectedTags,
        destinationFacility: 'Midwest Regional Processing Plant #04',
        carrierName: 'AgriLogistics Cold-Chain Transport'
      });
      setPermitResult(response.data);
    } catch (err) {
      if (err.response && err.response.data) {
        setErrorMsg(err.response.data.message || 'Permit request rejected by Gatekeeper.');
      } else {
        setErrorMsg('Error communicating with Shipment Gatekeeper Service.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-slate-900 text-white p-5 rounded-2xl border border-slate-800 flex justify-between items-center">
        <div>
          <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-amber-400 bg-amber-950/80 px-2.5 py-1 rounded-full border border-amber-500/40">
            DETERMINISTIC LOGISTICS GATEKEEPER
          </span>
          <h2 className="text-xl font-bold mt-2">Market Shipment Permit Generator</h2>
          <p className="text-xs text-slate-300 font-mono mt-1">
            Condition: ANY Animal == LOCKED &rarr; 400 Bad Request | ALL Animals == SAFE &rarr; 200 OK
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Panel: Animal Selection */}
        <div className="lg:col-span-7 bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-4">
          <div className="flex justify-between items-center border-b border-slate-100 pb-3">
            <h3 className="font-bold text-slate-900 text-sm">Select Livestock Batch</h3>
            <div className="flex gap-2">
              <button
                onClick={() => setSelectedTags(['COW-311', 'COW-312', 'HEN-222'])}
                className="px-2.5 py-1 bg-emerald-50 text-emerald-800 border border-emerald-300 rounded-lg text-xs font-semibold"
              >
                Safe Batch (Pass)
              </button>
              <button
                onClick={() => setSelectedTags(['COW-311', 'COW-804', 'PIG-045'])}
                className="px-2.5 py-1 bg-rose-50 text-rose-800 border border-rose-300 rounded-lg text-xs font-semibold"
              >
                Include Locked (Trigger 400)
              </button>
            </div>
          </div>

          <div className="space-y-2">
            {initialHerd.map(animal => {
              const isSelected = selectedTags.includes(animal.tagId);
              return (
                <div
                  key={animal.tagId}
                  onClick={() => toggleTag(animal.tagId)}
                  className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition ${
                    isSelected ? 'bg-slate-50 border-forest' : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => {}}
                      className="rounded"
                    />
                    <div>
                      <strong className="font-mono text-sm text-slate-900">{animal.tagId}</strong>
                      <span className="text-xs text-slate-500 ml-2">({animal.species} · {animal.barn})</span>
                    </div>
                  </div>
                  <div>
                    {animal.status === 'LOCKED' ? (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800">
                        🔴 LOCKED
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                        🟢 SAFE
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Panel: Gatekeeper Evaluation Engine */}
        <div className="lg:col-span-5 bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-4">
          <div className="border-b border-slate-100 pb-3 flex justify-between items-center">
            <h3 className="font-bold text-slate-900 text-sm">Gatekeeper Evaluation</h3>
            <span className="font-mono text-xs font-bold text-slate-700">Batch Size: {selectedTags.length}</span>
          </div>

          {/* Banner Evaluation */}
          {selectedTags.length === 0 ? (
            <div className="p-6 text-center text-slate-400 bg-slate-50 rounded-xl text-xs">
              Select livestock tags to evaluate permit status.
            </div>
          ) : !isBatchCompliant ? (
            <div className="bg-rose-50 border-2 border-rose-500 rounded-2xl p-5">
              <div className="flex items-start gap-3">
                <AlertOctagon className="w-8 h-8 text-rose-600 flex-shrink-0" />
                <div>
                  <h4 className="font-black text-rose-800 text-sm uppercase">PERMIT DENIED — 400 BAD REQUEST</h4>
                  <p className="text-xs text-rose-700 font-bold mt-1">
                    {lockedInBatch.length} animal(s) in batch currently LOCKED under mandatory withdrawal.
                  </p>
                </div>
              </div>
              <div className="mt-3 pt-3 border-t border-rose-200 space-y-1 text-xs font-mono text-rose-800">
                <span>Violating Tags:</span>
                {lockedInBatch.map(l => (
                  <div key={l.tagId} className="bg-white p-2 rounded border border-rose-300 flex justify-between">
                    <span>{l.tagId}</span>
                    <span className="text-[10px] text-rose-600 font-bold">WITHDRAWAL HOLD</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="bg-emerald-50 border-2 border-emerald-500 rounded-2xl p-5">
              <div className="flex items-start gap-3">
                <CheckCircle2 className="w-8 h-8 text-emerald-600 flex-shrink-0" />
                <div>
                  <h4 className="font-black text-emerald-800 text-sm uppercase">ALL {selectedTags.length} ANIMALS SAFE</h4>
                  <p className="text-xs text-emerald-700 mt-1">
                    HTTP 200 OK &bull; Zero chemical residues detected &bull; Clearance validated.
                  </p>
                </div>
              </div>
            </div>
          )}

          {errorMsg && (
            <div className="p-3 bg-rose-50 border border-rose-300 text-rose-800 rounded-xl text-xs font-medium">
              {errorMsg}
            </div>
          )}

          {permitResult && (
            <div className="p-3 bg-emerald-100 border border-emerald-300 text-emerald-900 rounded-xl text-xs font-mono space-y-1">
              <div><strong>Permit ID:</strong> {permitResult.permitId}</div>
              <div><strong>Fabric TX:</strong> {permitResult.fabricTxHash}</div>
              <div className="text-[10px] text-emerald-700">Digital X.509 Seal Verified On-Chain</div>
            </div>
          )}

          {/* Action Button: Disabled if ANY animal is locked */}
          <button
            onClick={handleRequestPermit}
            disabled={!isBatchCompliant || loading}
            className="w-full py-3 bg-forest hover:bg-forest-light text-white font-bold rounded-xl text-xs uppercase tracking-wider transition shadow-md disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            <Send className="w-4 h-4" />
            <span>Sign & Broadcast Permit to Hyperledger Fabric</span>
          </button>
        </div>
      </div>
    </div>
  );
};
