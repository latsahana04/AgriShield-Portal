import React, { useState } from 'react';
import { Blocks, Key, CheckCircle, X, ShieldAlert } from 'lucide-react';

export const AuditorExplorerView = () => {
  const [selectedTx, setSelectedTx] = useState(null);

  // Mock block stream representing Hyperledger Fabric 2.5
  const blocks = [
    {
      blockNumber: 1045,
      txId: '0x8f2a1b9c0d3e4f5a',
      fullHash: '8f2a1b9c0d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a',
      animalTagId: 'COW-805',
      drug: 'Ceftiofur Sodium',
      dose: '12 ml',
      vetLicense: 'VET-USA-4402',
      timestamp: '2026-09-29T18:30:00Z',
      peerEndorsements: ['peer0.farmorg.farm.com', 'peer0.vetassoc.org'],
      raftLeader: 'orderer.example.com:7050'
    },
    {
      blockNumber: 1044,
      txId: '0x4e21c7aa90bb8210',
      fullHash: '4e21c7aa90bb8210a34b2c1d0f9e8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f',
      animalTagId: 'PIG-102',
      drug: 'Oxytetracycline 200',
      dose: '8 ml',
      vetLicense: 'VET-USA-4402',
      timestamp: '2026-09-29T17:15:00Z',
      peerEndorsements: ['peer0.farmorg.farm.com', 'peer0.vetassoc.org'],
      raftLeader: 'orderer.example.com:7050'
    },
    {
      blockNumber: 1043,
      txId: '0x9b03f1e662aa0914',
      fullHash: '9b03f1e662aa0914c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4',
      animalTagId: 'HEN-221',
      drug: 'Oxytetracycline 200',
      dose: '0.4 ml',
      vetLicense: 'VET-USA-4402',
      timestamp: '2026-09-29T15:40:00Z',
      peerEndorsements: ['peer0.farmorg.farm.com', 'peer0.vetassoc.org'],
      raftLeader: 'orderer.example.com:7050'
    }
  ];

  return (
    <div className="space-y-6">
      <div className="bg-purple-900 text-white p-5 rounded-2xl border border-purple-800 flex justify-between items-center">
        <div>
          <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-purple-300 bg-purple-950/80 px-2.5 py-1 rounded-full border border-purple-500/40">
            GOVERNMENT AUDIT NODE
          </span>
          <h2 className="text-xl font-bold mt-2">Hyperledger Fabric 2.5 Blockchain Explorer</h2>
          <p className="text-xs text-purple-200 font-mono mt-1">Channel: farmchannel &bull; Raft Consensus Sync: 100%</p>
        </div>
      </div>

      {/* Top Metrics */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm text-center">
          <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">Block Height</span>
          <strong className="text-2xl font-mono text-slate-900 mt-1 block">#1045</strong>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm text-center">
          <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">Endorsing Peers</span>
          <strong className="text-2xl font-mono text-emerald-600 mt-1 block">2 / 2</strong>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm text-center">
          <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">World State</span>
          <strong className="text-2xl font-mono text-blue-600 mt-1 block">CouchDB</strong>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm text-center">
          <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">Integrity</span>
          <strong className="text-2xl font-mono text-emerald-600 mt-1 block">100%</strong>
        </div>
      </div>

      {/* Block Stream Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-200 bg-slate-50 font-bold text-xs text-slate-700">
          Live Committed Blocks
        </div>
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr class="bg-slate-100 text-slate-600 font-bold">
              <th className="p-3">Block #</th>
              <th className="p-3">Transaction ID</th>
              <th className="p-3">Animal Tag</th>
              <th className="p-3">Drug</th>
              <th className="p-3">Timestamp (UTC)</th>
              <th className="p-3 text-right">Verification</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
            {blocks.map(b => (
              <tr key={b.blockNumber} className="hover:bg-slate-50">
                <td className="p-3 text-blue-600 font-bold">#{b.blockNumber}</td>
                <td className="p-3 font-bold text-slate-800">{b.txId}</td>
                <td className="p-3 font-sans font-bold text-slate-900">{b.animalTagId}</td>
                <td className="p-3 font-sans text-slate-700">{b.drug}</td>
                <td className="p-3 text-slate-500">{new Date(b.timestamp).toUTCString()}</td>
                <td className="p-3 text-right font-sans">
                  <button
                    onClick={() => setSelectedTx(b)}
                    className="px-2.5 py-1 bg-slate-800 text-white rounded-lg text-xs font-semibold hover:bg-slate-700"
                  >
                    Inspect Payload
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Raw JSON Payload Modal */}
      {selectedTx && (
        <div className="fixed inset-0 bg-slate-900/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex justify-between items-center border-b pb-3">
              <div>
                <span className="text-[10px] text-purple-600 font-mono font-bold uppercase">CouchDB Document</span>
                <h4 className="font-mono font-bold text-slate-900 text-base">{selectedTx.txId}</h4>
              </div>
              <button onClick={() => setSelectedTx(null)} className="text-slate-400 hover:text-slate-700">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-2">
              <span className="text-xs font-bold text-slate-700 block">Raw JSON State on Fabric World State:</span>
              <pre className="p-3.5 bg-slate-900 text-emerald-400 rounded-xl font-mono text-xs overflow-x-auto">
{JSON.stringify({
  treatmentId: selectedTx.txId,
  animalTagId: selectedTx.animalTagId,
  medication: selectedTx.drug,
  dosage: selectedTx.dose,
  vetLicenseId: selectedTx.vetLicense,
  timestamp: selectedTx.timestamp,
  peerEndorsements: selectedTx.peerEndorsements,
  consensusProof: `RaftSignature_${selectedTx.fullHash.substring(0, 16)}`
}, null, 2)}
              </pre>
            </div>

            <div className="p-3 bg-emerald-50 border border-emerald-300 rounded-xl text-xs text-emerald-900 flex items-center gap-2">
              <CheckCircle className="w-5 h-5 text-emerald-600 flex-shrink-0" />
              <span>Cryptographic hash matches peer ledger state. Record is 100% immutable and verified on-chain.</span>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedTx(null)}
                className="px-4 py-2 bg-slate-800 text-white rounded-lg text-xs font-semibold"
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
