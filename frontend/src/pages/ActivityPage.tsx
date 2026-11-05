import React, { useState, useEffect } from 'react';
import { 
  History, 
  Search, 
  Filter, 
  User, 
  RefreshCw, 
  FileText,
  Clock,
  ShieldCheck
} from 'lucide-react';
import api from '../lib/api';

interface AuditLogEntry {
  id: string;
  organizationId: string;
  actorId?: string;
  actorEmail?: string;
  entityType: string;
  entityId: string;
  action: string;
  beforeState?: string;
  afterState?: string;
  ipAddress?: string;
  createdAt: string;
}

export const ActivityPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedEntity, setSelectedEntity] = useState<string>('ALL');
  const [selectedLog, setSelectedLog] = useState<AuditLogEntry | null>(null);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      const res = await api.get('/audit/recent');
      setLogs(res.data || []);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const filteredLogs = logs.filter(log => {
    const matchesSearch = 
      (log.actorEmail?.toLowerCase().includes(searchTerm.toLowerCase()) || false) ||
      log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.entityType.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.entityId.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesFilter = selectedEntity === 'ALL' || log.entityType === selectedEntity;
    return matchesSearch && matchesFilter;
  });

  const getActionBadge = (action: string) => {
    if (action.includes('CREATE') || action.includes('CONFIRM')) {
      return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">Created / Confirmed</span>;
    }
    if (action.includes('UPDATE') || action.includes('BULK')) {
      return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">Updated</span>;
    }
    if (action.includes('DELETE') || action.includes('CANCEL')) {
      return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800">Cancelled / Removed</span>;
    }
    return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">{action}</span>;
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-lg bg-emerald-50 text-emerald-800">
              <History className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Activity History & Audit Trail</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Complete, tamper-proof record of all inventory updates, rate adjustments, mappings, and system changes.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchLogs}
            disabled={loading}
            className="inline-flex items-center gap-2 px-3.5 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition shadow-sm"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col md:flex-row gap-4 justify-between items-center">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
          <input
            type="text"
            placeholder="Search by user email, action, entity or room ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 text-sm rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-700 focus:border-transparent"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={selectedEntity}
            onChange={(e) => setSelectedEntity(e.target.value)}
            className="text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-emerald-700"
          >
            <option value="ALL">All Categories</option>
            <option value="DAILY_INVENTORY">Daily Inventory</option>
            <option value="DAILY_RATE">Daily Rates</option>
            <option value="RESERVATION">Reservations</option>
            <option value="CHANNEL_MAPPING">Channel Mappings</option>
            <option value="PROPERTY">Property Settings</option>
          </select>
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto text-emerald-800 mb-3" />
            <p className="text-sm font-medium">Loading activity audit log...</p>
          </div>
        ) : filteredLogs.length === 0 ? (
          <div className="p-12 text-center">
            <ShieldCheck className="w-12 h-12 text-emerald-800 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-900">No activity logs found</h3>
            <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
              Any changes made to inventory, rates, or bookings will appear here in chronological order.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4 font-semibold">Timestamp</th>
                  <th className="py-3 px-4 font-semibold">User / Actor</th>
                  <th className="py-3 px-4 font-semibold">Category</th>
                  <th className="py-3 px-4 font-semibold">Action</th>
                  <th className="py-3 px-4 font-semibold">Target Entity</th>
                  <th className="py-3 px-4 font-semibold text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50 transition">
                    <td className="py-3.5 px-4 text-xs text-slate-500 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-slate-400" />
                        {new Date(log.createdAt).toLocaleString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                          second: '2-digit'
                        })}
                      </div>
                    </td>
                    <td className="py-3.5 px-4 font-medium text-slate-900 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 text-xs">
                          <User className="w-3.5 h-3.5" />
                        </div>
                        <span>{log.actorEmail || 'System Worker'}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                        {log.entityType}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {getActionBadge(log.action)}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-xs text-slate-600 max-w-xs truncate">
                      {log.entityId}
                    </td>
                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <button
                        onClick={() => setSelectedLog(log)}
                        className="text-xs font-semibold text-emerald-800 hover:text-emerald-900 hover:underline"
                      >
                        Inspect Diff
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Diff Inspector Modal */}
      {selectedLog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="p-5 border-b border-slate-200 flex items-center justify-between bg-slate-50">
              <div className="flex items-center gap-2">
                <FileText className="w-5 h-5 text-emerald-800" />
                <h3 className="text-base font-bold text-slate-900">Audit Change Inspector</h3>
              </div>
              <button
                onClick={() => setSelectedLog(null)}
                className="text-slate-400 hover:text-slate-600 text-lg font-bold"
              >
                ×
              </button>
            </div>

            <div className="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
              <div className="grid grid-cols-2 gap-4 text-xs bg-slate-50 p-3.5 rounded-lg border border-slate-200">
                <div>
                  <span className="text-slate-400">Timestamp:</span>
                  <p className="font-medium text-slate-800">{new Date(selectedLog.createdAt).toLocaleString()}</p>
                </div>
                <div>
                  <span className="text-slate-400">Actor:</span>
                  <p className="font-medium text-slate-800">{selectedLog.actorEmail || 'Background Job'}</p>
                </div>
                <div>
                  <span className="text-slate-400">Entity:</span>
                  <p className="font-medium text-slate-800">{selectedLog.entityType} ({selectedLog.entityId})</p>
                </div>
                <div>
                  <span className="text-slate-400">IP Address:</span>
                  <p className="font-medium text-slate-800">{selectedLog.ipAddress || 'Internal'}</p>
                </div>
              </div>

              <div>
                <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Previous Value (Before)</h4>
                <pre className="p-3 bg-rose-50/50 border border-rose-100 rounded-lg text-xs font-mono text-rose-900 whitespace-pre-wrap break-all">
                  {selectedLog.beforeState || 'None (New Record)'}
                </pre>
              </div>

              <div>
                <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">New Value (After)</h4>
                <pre className="p-3 bg-emerald-50/50 border border-emerald-100 rounded-lg text-xs font-mono text-emerald-900 whitespace-pre-wrap break-all">
                  {selectedLog.afterState || 'None (Deleted)'}
                </pre>
              </div>
            </div>

            <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-end">
              <button
                onClick={() => setSelectedLog(null)}
                className="px-4 py-2 text-sm font-medium bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-lg transition"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
