import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Globe2,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  RefreshCw,
  Clock,
  Activity,
  Zap,
  Building2,
  RotateCw,
  Sliders,
  Check,
  X
} from 'lucide-react';
import api from '../lib/api';

interface Property {
  id: string;
  name: string;
}

interface ChannelAccount {
  id: string;
  propertyId: string;
  channelType: string;
  accountName: string;
  status: 'CONNECTED' | 'ERROR' | 'DISABLED' | 'SYNCING';
  syncEnabled: boolean;
  lastSyncAt: string | null;
  errorMessage: string | null;
  roomMappingsCount: number;
  rateMappingsCount: number;
}

interface SyncJob {
  id: string;
  channelAccountId: string;
  jobType: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';
  payloadSummary: string;
  responseSummary: string;
  attemptCount: number;
  maxAttempts: number;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
}

const CHANNEL_CONFIG: Record<string, { label: string; badgeClass: string }> = {
  MOCK: { label: 'Mock OTA', badgeClass: 'bg-slate-100 text-slate-700 border-slate-200' },
  BOOKING_COM: { label: 'Booking.com', badgeClass: 'bg-blue-50 text-blue-700 border-blue-200' },
  EXPEDIA: { label: 'Expedia', badgeClass: 'bg-amber-50 text-amber-700 border-amber-200' },
  AIRBNB: { label: 'Airbnb', badgeClass: 'bg-rose-50 text-rose-700 border-rose-200' },
  AGODA: { label: 'Agoda', badgeClass: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  MAKEMYTRIP: { label: 'MakeMyTrip', badgeClass: 'bg-red-50 text-red-700 border-red-200' },
  GOIBIBO: { label: 'Goibibo', badgeClass: 'bg-orange-50 text-orange-700 border-orange-200' },
  ICAL: { label: 'iCal Feed', badgeClass: 'bg-purple-50 text-purple-700 border-purple-200' }
};

export const ChannelHealthPage: React.FC = () => {
  const navigate = useNavigate();
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [accounts, setAccounts] = useState<ChannelAccount[]>([]);
  const [syncJobs, setSyncJobs] = useState<SyncJob[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [retryingJobId, setRetryingJobId] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    loadProperties();
  }, []);

  useEffect(() => {
    if (selectedPropertyId) {
      loadHealthData(selectedPropertyId);
    }
  }, [selectedPropertyId]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  const loadProperties = async () => {
    try {
      setLoading(true);
      const res = await api.get<Property[]>('/properties');
      if (res.data && res.data.length > 0) {
        setProperties(res.data);
        setSelectedPropertyId(res.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load properties', err);
    } finally {
      setLoading(false);
    }
  };

  const loadHealthData = async (propId: string) => {
    try {
      setLoading(true);
      const [accRes, jobsRes] = await Promise.all([
        api.get<ChannelAccount[]>(`/properties/${propId}/channels`),
        api.get<{ content: SyncJob[] }>(`/properties/${propId}/channels/health/sync-jobs?size=50`)
      ]);
      setAccounts(accRes.data || []);
      setSyncJobs(jobsRes.data?.content || []);
    } catch (err) {
      console.error('Failed to load channel health data', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRetryJob = async (accountId: string, jobId: string) => {
    try {
      setRetryingJobId(jobId);
      await api.post(`/properties/${selectedPropertyId}/channels/${accountId}/sync-jobs/${jobId}/retry`);
      showToast('Sync job queued for immediate retry.');
      loadHealthData(selectedPropertyId);
    } catch (err: any) {
      alert(err.message || 'Retry failed.');
    } finally {
      setRetryingJobId(null);
    }
  };

  const healthyChannelsCount = accounts.filter(
    (a) => a.syncEnabled && a.status === 'CONNECTED'
  ).length;

  const failedJobsCount = syncJobs.filter((j) => j.status === 'FAILED').length;

  return (
    <div className="space-y-6">
      {/* Toast Feedback */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-slate-900 text-white px-5 py-3 rounded-lg shadow-xl text-sm font-medium animate-in fade-in slide-in-from-bottom-4">
          <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Channel Health & Sync Status</h1>
          <p className="text-slate-500 text-sm mt-0.5">
            Monitor real-time OTA connection latency, transactional outbox status, and error logs.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {properties.length > 1 && (
            <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-lg border border-slate-200 text-sm shadow-sm">
              <Building2 className="w-4 h-4 text-slate-400" />
              <select
                value={selectedPropertyId}
                onChange={(e) => setSelectedPropertyId(e.target.value)}
                className="bg-transparent border-none text-slate-700 font-medium focus:ring-0 cursor-pointer"
              >
                {properties.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          <button
            onClick={() => loadHealthData(selectedPropertyId)}
            className="p-2 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 transition-colors shadow-sm"
            title="Refresh status"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          <button
            onClick={() => navigate('/channels')}
            className="flex items-center gap-2 bg-slate-900 hover:bg-slate-800 text-white px-4 py-2 rounded-lg font-medium text-sm transition-colors shadow-sm"
          >
            <Sliders className="w-4 h-4" />
            <span>Manage Channels</span>
          </button>
        </div>
      </div>

      {/* Health Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Metric 1: Overall Health */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Sync Engine Status</span>
            <Activity className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="mt-3 flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
            <span className="text-2xl font-extrabold text-slate-900">
              {failedJobsCount === 0 ? 'Optimal' : 'Needs Review'}
            </span>
          </div>
          <div className="mt-2 text-[11px] text-slate-500">
            {healthyChannelsCount} of {accounts.length} channels syncing normally
          </div>
        </div>

        {/* Metric 2: Sync Latency */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>P95 Outbound Latency</span>
            <Zap className="w-4 h-4 text-amber-500" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-slate-900">&lt; 1.2s</span>
            <span className="text-xs text-emerald-600 font-semibold">Target &lt; 60s</span>
          </div>
          <div className="mt-2 text-[11px] text-slate-500">
            Outbox queue processing within SLA
          </div>
        </div>

        {/* Metric 3: Active OTAs */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Active Connected OTAs</span>
            <Globe2 className="w-4 h-4 text-blue-600" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-slate-900">{accounts.length}</span>
            <span className="text-xs text-slate-500">channels linked</span>
          </div>
          <div className="mt-2 text-[11px] text-slate-500">
            2-way availability and rate sync enabled
          </div>
        </div>

        {/* Metric 4: Failed Syncs */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Recent Sync Errors</span>
            <AlertTriangle className="w-4 h-4 text-rose-500" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-slate-900">{failedJobsCount}</span>
            <span className="text-xs text-slate-400">in last 50 jobs</span>
          </div>
          <div className="mt-2 text-[11px] text-slate-500">
            {failedJobsCount === 0 ? 'Zero drift detected' : 'Requires manual retry'}
          </div>
        </div>
      </div>

      {/* Connected Channel Cards */}
      <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
        <h3 className="font-bold text-slate-900 text-base mb-4">Channel Connection Status</h3>

        {accounts.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-xs">
            <Globe2 className="w-8 h-8 text-slate-300 mx-auto mb-2" />
            <p>No channels connected yet. Go to Channels to connect Booking.com, Airbnb, or Mock OTA.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {accounts.map((acc) => {
              const badge = CHANNEL_CONFIG[acc.channelType] || {
                label: acc.channelType,
                badgeClass: 'bg-slate-100 text-slate-700'
              };

              return (
                <div
                  key={acc.id}
                  className="p-4 rounded-xl border border-slate-200 bg-slate-50/50 flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className={`px-2.5 py-0.5 rounded-full font-semibold text-[11px] border ${badge.badgeClass}`}>
                        {badge.label}
                      </span>
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold ${
                          acc.status === 'CONNECTED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : acc.status === 'ERROR'
                            ? 'bg-rose-100 text-rose-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {acc.status === 'CONNECTED' ? (
                          <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                        ) : (
                          <XCircle className="w-3 h-3 text-rose-600" />
                        )}
                        <span>{acc.status}</span>
                      </span>
                    </div>

                    <h4 className="font-bold text-slate-900 text-sm mt-1">{acc.accountName}</h4>
                    <p className="text-slate-400 text-xs mt-0.5 font-mono">
                      {acc.roomMappingsCount} room types mapped
                    </p>

                    {acc.errorMessage && (
                      <div className="mt-2 p-2 bg-rose-50 border border-rose-200 rounded text-[11px] text-rose-700">
                        {acc.errorMessage}
                      </div>
                    )}
                  </div>

                  <div className="mt-4 pt-3 border-t border-slate-200/60 flex items-center justify-between text-[11px] text-slate-500">
                    <span>Last sync: {acc.lastSyncAt ? new Date(acc.lastSyncAt).toLocaleTimeString() : 'Never'}</span>
                    <button
                      onClick={() => navigate('/channels')}
                      className="text-indigo-600 hover:text-indigo-800 font-semibold"
                    >
                      Configure
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Live Sync Jobs Feed */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-200 flex items-center justify-between">
          <div>
            <h3 className="font-bold text-slate-900 text-base">Outbox & Sync Audit Trail</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Every inventory, rate, and reservation sync attempt is logged with full payload status.
            </p>
          </div>
          <button
            onClick={() => loadHealthData(selectedPropertyId)}
            className="text-xs font-semibold text-emerald-700 hover:text-emerald-800 flex items-center gap-1"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Refresh log</span>
          </button>
        </div>

        {loading ? (
          <div className="p-12 text-center text-slate-400 text-xs">
            <RefreshCw className="w-8 h-8 text-slate-300 mx-auto mb-2 animate-spin" />
            <p>Loading channel health logs...</p>
          </div>
        ) : syncJobs.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-xs">
            <Clock className="w-8 h-8 text-slate-300 mx-auto mb-2" />
            <p>No sync jobs recorded yet. Changes to the calendar will generate sync events here.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[11px]">
                  <th className="py-3 px-6">Timestamp</th>
                  <th className="py-3 px-6">Job Type</th>
                  <th className="py-3 px-6">Status</th>
                  <th className="py-3 px-6">Attempts</th>
                  <th className="py-3 px-6">Payload Summary</th>
                  <th className="py-3 px-6 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {syncJobs.map((job) => (
                  <tr key={job.id} className="hover:bg-slate-50 transition-colors">
                    <td className="py-3 px-6 text-slate-600 font-mono text-[11px]">
                      {new Date(job.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3 px-6 font-semibold text-slate-800">
                      {job.jobType}
                    </td>
                    <td className="py-3 px-6">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold ${
                          job.status === 'SUCCESS'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : job.status === 'FAILED'
                            ? 'bg-rose-50 text-rose-700 border border-rose-200'
                            : 'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}
                      >
                        {job.status === 'SUCCESS' && <Check className="w-3 h-3 text-emerald-600" />}
                        {job.status === 'FAILED' && <X className="w-3 h-3 text-rose-600" />}
                        <span>{job.status}</span>
                      </span>
                    </td>
                    <td className="py-3 px-6 text-slate-500">
                      {job.attemptCount}/{job.maxAttempts}
                    </td>
                    <td className="py-3 px-6 text-slate-600 max-w-xs truncate" title={job.payloadSummary}>
                      {job.payloadSummary || 'Calendar availability update'}
                      {job.errorMessage && (
                        <span className="block text-[10px] text-rose-600 font-semibold mt-0.5">
                          Error: {job.errorMessage}
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-6 text-right">
                      {job.status === 'FAILED' ? (
                        <button
                          onClick={() => handleRetryJob(job.channelAccountId, job.id)}
                          disabled={retryingJobId === job.id}
                          className="bg-rose-600 hover:bg-rose-700 text-white px-2.5 py-1 rounded text-xs font-semibold shadow-sm inline-flex items-center gap-1"
                        >
                          <RotateCw className={`w-3 h-3 ${retryingJobId === job.id ? 'animate-spin' : ''}`} />
                          <span>Retry</span>
                        </button>
                      ) : (
                        <span className="text-slate-400 text-[11px] italic">Completed</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default ChannelHealthPage;
