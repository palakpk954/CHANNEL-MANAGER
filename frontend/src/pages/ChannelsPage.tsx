import React, { useState, useEffect } from 'react';
import {
  Globe2,
  Plus,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  PauseCircle,
  Sliders,
  History,
  Trash2,
  X,
  Check,
  ShieldCheck,
  Zap
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
  createdAt: string;
}

interface MappingOptions {
  internalRooms: Array<{ id: string; name: string; code: string; totalUnits: number }>;
  internalRates: Array<{ id: string; name: string; code: string; roomTypeId: string }>;
  channelRooms: Array<{ channelRoomId: string; channelRoomName: string }>;
  channelRates: Array<{ channelRateId: string; channelRateName: string; channelRoomId: string }>;
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

const CHANNEL_DEFINITIONS: Record<string, { name: string; desc: string; badge: string; color: string }> = {
  MOCK: { name: 'Mock Channel', desc: 'Simulated OTA for instant testing & demonstrations', badge: 'Demo Ready', color: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  BOOKING_COM: { name: 'Booking.com', desc: 'Global online travel agency & partner network', badge: 'Direct XML', color: 'bg-blue-50 text-blue-700 border-blue-200' },
  EXPEDIA: { name: 'Expedia Partner Central', desc: 'Global lodging distribution via EPC API', badge: 'EPC Partner', color: 'bg-amber-50 text-amber-700 border-amber-200' },
  AIRBNB: { name: 'Airbnb', desc: 'Vacation rental & urban apartment listings', badge: 'API Sync', color: 'bg-rose-50 text-rose-700 border-rose-200' },
  AGODA: { name: 'Agoda YCS', desc: 'Asia-Pacific focused accommodation marketplace', badge: 'YCS API', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  MAKEMYTRIP: { name: 'MakeMyTrip', desc: 'Leading Indian OTA & corporate travel platform', badge: 'IngoMMT', color: 'bg-red-50 text-red-700 border-red-200' },
  GOIBIBO: { name: 'Goibibo', desc: 'High-volume domestic hotel booking network', badge: 'IngoMMT', color: 'bg-orange-50 text-orange-700 border-orange-200' },
  ICAL: { name: 'iCal Two-Way', desc: 'Universal calendar sync (.ics) for Airbnb/VRBO', badge: 'Universal', color: 'bg-purple-50 text-purple-700 border-purple-200' }
};

export const ChannelsPage: React.FC = () => {
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [channels, setChannels] = useState<ChannelAccount[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [actionMessage, setActionMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  // Connect Modal state
  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false);
  const [connectStep, setConnectStep] = useState<1 | 2>(1);
  const [selectedChannelType, setSelectedChannelType] = useState<string>('MOCK');
  const [accountName, setAccountName] = useState('');
  const [apiKey, setApiKey] = useState('demo-key-123');
  const [hotelId, setHotelId] = useState('hotel-999');
  const [testingConnection, setTestingConnection] = useState(false);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // Mapping Modal state
  const [mappingChannel, setMappingChannel] = useState<ChannelAccount | null>(null);
  const [mappingOptions, setMappingOptions] = useState<MappingOptions | null>(null);
  const [selectedRoomMappings, setSelectedRoomMappings] = useState<Record<string, string>>({});
  const [selectedRateMappings, setSelectedRateMappings] = useState<Record<string, string>>({});
  const [savingMappings, setSavingMappings] = useState(false);

  // Sync History Drawer state
  const [historyChannel, setHistoryChannel] = useState<ChannelAccount | null>(null);
  const [syncJobs, setSyncJobs] = useState<SyncJob[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // Load properties
  useEffect(() => {
    fetchProperties();
  }, []);

  // Load channels when property selected
  useEffect(() => {
    if (selectedPropertyId) {
      fetchChannels(selectedPropertyId);
    }
  }, [selectedPropertyId]);

  const fetchProperties = async () => {
    try {
      const res = await api.get('/api/v1/properties');
      setProperties(res.data);
      if (res.data.length > 0) {
        setSelectedPropertyId(res.data[0].id);
      } else {
        setIsLoading(false);
      }
    } catch {
      setActionMessage({ text: 'Could not load properties.', type: 'error' });
      setIsLoading(false);
    }
  };

  const fetchChannels = async (propertyId: string) => {
    setIsLoading(true);
    try {
      const res = await api.get(`/api/v1/properties/${propertyId}/channels`);
      setChannels(res.data);
    } catch {
      setActionMessage({ text: 'Could not load connected channels.', type: 'error' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleTestConnection = async () => {
    setTestingConnection(true);
    setTestResult(null);
    try {
      const res = await api.post('/api/v1/channels/test-connection', {
        channelType: selectedChannelType,
        credentials: { apiKey, hotelId }
      });
      setTestResult({ success: res.data.success, message: res.data.message });
    } catch {
      setTestResult({ success: false, message: 'Connection test failed. Check credentials and network.' });
    } finally {
      setTestingConnection(false);
    }
  };

  const handleCreateAccount = async () => {
    if (!accountName.trim()) {
      setActionMessage({ text: 'Please enter an account name.', type: 'error' });
      return;
    }
    try {
      await api.post(`/api/v1/properties/${selectedPropertyId}/channels`, {
        channelType: selectedChannelType,
        accountName: accountName.trim(),
        credentials: { apiKey, hotelId }
      });
      setActionMessage({ text: `Connected ${accountName} successfully.`, type: 'success' });
      setIsConnectModalOpen(false);
      setAccountName('');
      setTestResult(null);
      fetchChannels(selectedPropertyId);
    } catch {
      setActionMessage({ text: 'Failed to connect channel account.', type: 'error' });
    }
  };

  const handleSyncNow = async (channelId: string, name: string) => {
    try {
      setActionMessage({ text: `Triggering sync for ${name}...`, type: 'success' });
      await api.post(`/api/v1/properties/${selectedPropertyId}/channels/${channelId}/sync-now`);
      setActionMessage({ text: `Sync completed for ${name}. Channel updated.`, type: 'success' });
      fetchChannels(selectedPropertyId);
    } catch {
      setActionMessage({ text: `Sync failed for ${name}.`, type: 'error' });
    }
  };

  const openMappingModal = async (channel: ChannelAccount) => {
    setMappingChannel(channel);
    try {
      const [optRes, mapRes] = await Promise.all([
        api.get(`/api/v1/properties/${selectedPropertyId}/channels/${channel.id}/mapping-options`),
        api.get(`/api/v1/properties/${selectedPropertyId}/channels/${channel.id}/mappings`)
      ]);
      setMappingOptions(optRes.data);

      const roomMap: Record<string, string> = {};
      mapRes.data.roomMappings.forEach((m: { roomTypeId: string; channelRoomId: string }) => {
        roomMap[m.roomTypeId] = m.channelRoomId;
      });
      setSelectedRoomMappings(roomMap);

      const rateMap: Record<string, string> = {};
      mapRes.data.rateMappings.forEach((m: { ratePlanId: string; channelRateId: string }) => {
        rateMap[m.ratePlanId] = m.channelRateId;
      });
      setSelectedRateMappings(rateMap);
    } catch {
      setActionMessage({ text: 'Could not load mapping options.', type: 'error' });
    }
  };

  const handleSaveMappings = async () => {
    if (!mappingChannel || !mappingOptions) return;
    setSavingMappings(true);

    try {
      const roomPayload = Object.entries(selectedRoomMappings)
        .filter(([, chId]) => Boolean(chId))
        .map(([roomTypeId, channelRoomId]) => {
          const chRoom = mappingOptions.channelRooms.find(r => r.channelRoomId === channelRoomId);
          return {
            roomTypeId,
            channelRoomId,
            channelRoomName: chRoom?.channelRoomName || channelRoomId,
            active: true
          };
        });

      const ratePayload = Object.entries(selectedRateMappings)
        .filter(([, chId]) => Boolean(chId))
        .map(([ratePlanId, channelRateId]) => {
          const chRate = mappingOptions.channelRates.find(r => r.channelRateId === channelRateId);
          return {
            ratePlanId,
            channelRateId,
            channelRateName: chRate?.channelRateName || channelRateId,
            active: true
          };
        });

      if (roomPayload.length > 0) {
        await api.put(`/api/v1/properties/${selectedPropertyId}/channels/${mappingChannel.id}/room-mappings`, {
          mappings: roomPayload
        });
      }

      if (ratePayload.length > 0) {
        await api.put(`/api/v1/properties/${selectedPropertyId}/channels/${mappingChannel.id}/rate-mappings`, {
          mappings: ratePayload
        });
      }

      setActionMessage({ text: 'Room & rate mappings saved. Outbound sync triggered.', type: 'success' });
      setMappingChannel(null);
      fetchChannels(selectedPropertyId);
    } catch {
      setActionMessage({ text: 'Failed to save mappings.', type: 'error' });
    } finally {
      setSavingMappings(false);
    }
  };

  const openHistoryDrawer = async (channel: ChannelAccount) => {
    setHistoryChannel(channel);
    setLoadingHistory(true);
    try {
      const res = await api.get(`/api/v1/properties/${selectedPropertyId}/channels/${channel.id}/sync-jobs`);
      setSyncJobs(res.data);
    } catch {
      setActionMessage({ text: 'Could not load sync history.', type: 'error' });
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleRetryJob = async (jobId: string) => {
    if (!historyChannel) return;
    try {
      await api.post(`/api/v1/properties/${selectedPropertyId}/channels/${historyChannel.id}/sync-jobs/${jobId}/retry`);
      setActionMessage({ text: 'Sync retry scheduled.', type: 'success' });
      const res = await api.get(`/api/v1/properties/${selectedPropertyId}/channels/${historyChannel.id}/sync-jobs`);
      setSyncJobs(res.data);
    } catch {
      setActionMessage({ text: 'Could not retry sync job.', type: 'error' });
    }
  };

  const handleDeleteAccount = async (channelId: string, name: string) => {
    if (!window.confirm(`Are you sure you want to disconnect ${name}? Outbound updates will stop.`)) return;
    try {
      await api.delete(`/api/v1/properties/${selectedPropertyId}/channels/${channelId}`);
      setActionMessage({ text: `Disconnected ${name}.`, type: 'success' });
      fetchChannels(selectedPropertyId);
    } catch {
      setActionMessage({ text: `Failed to disconnect ${name}.`, type: 'error' });
    }
  };

  return (
    <div className="space-y-6">
      {/* Toast Notification */}
      {actionMessage && (
        <div
          className={`p-4 rounded-xl flex items-center justify-between text-sm shadow-sm transition-all ${
            actionMessage.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
              : 'bg-rose-50 text-rose-800 border border-rose-200'
          }`}
        >
          <div className="flex items-center gap-2">
            {actionMessage.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            ) : (
              <AlertTriangle className="w-5 h-5 text-rose-600" />
            )}
            <span className="font-medium">{actionMessage.text}</span>
          </div>
          <button
            onClick={() => setActionMessage(null)}
            className="text-neutral-400 hover:text-neutral-700 p-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-2 border-b border-neutral-200">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2.5">
            <Globe2 className="w-6 h-6 text-brand-600" />
            Connected Channels
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Connect OTA channels to push availability and rates automatically with transactional outbox reliability.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {properties.length > 1 && (
            <select
              value={selectedPropertyId}
              onChange={(e) => setSelectedPropertyId(e.target.value)}
              className="px-3 py-2 text-sm border border-neutral-300 rounded-lg bg-white font-medium text-neutral-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              {properties.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          )}

          <button
            onClick={() => {
              setIsConnectModalOpen(true);
              setConnectStep(1);
              setSelectedChannelType('MOCK');
              setAccountName('Demo Mock OTA');
              setApiKey('demo-key-123');
              setHotelId('mock-hotel-1');
              setTestResult(null);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors"
          >
            <Plus className="w-4 h-4" />
            Connect channel
          </button>
        </div>
      </div>

      {/* Main Content */}
      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center text-neutral-500">
          <RefreshCw className="w-8 h-8 animate-spin text-brand-600 mb-3" />
          <p className="text-sm font-medium">Loading connected channels...</p>
        </div>
      ) : channels.length === 0 ? (
        <div className="bg-white rounded-2xl border border-neutral-200 p-12 text-center max-w-2xl mx-auto shadow-sm">
          <div className="w-16 h-16 bg-cream-100 rounded-full flex items-center justify-center mx-auto mb-4 text-brand-600">
            <Globe2 className="w-8 h-8" />
          </div>
          <h2 className="text-lg font-bold text-neutral-900">No channels connected yet</h2>
          <p className="text-sm text-neutral-500 mt-2 max-w-md mx-auto">
            Connect your first channel to start synchronizing room availability, rates, and restrictions across OTAs.
          </p>
          <div className="mt-6 flex justify-center gap-3">
            <button
              onClick={() => {
                setIsConnectModalOpen(true);
                setConnectStep(1);
                setSelectedChannelType('MOCK');
                setAccountName('Demo Mock OTA');
                setApiKey('demo-key-123');
                setHotelId('mock-hotel-1');
              }}
              className="px-5 py-2.5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-semibold rounded-xl shadow transition-colors flex items-center gap-2"
            >
              <Zap className="w-4 h-4" />
              Connect Mock Channel (1-click Demo)
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {channels.map((ch) => {
            const def = CHANNEL_DEFINITIONS[ch.channelType] || {
              name: ch.channelType,
              desc: 'OTA Channel Connection',
              badge: 'Channel',
              color: 'bg-neutral-100 text-neutral-800 border-neutral-200'
            };

            const isOk = ch.status === 'CONNECTED';
            const isError = ch.status === 'ERROR';

            return (
              <div
                key={ch.id}
                className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-6 flex flex-col justify-between hover:border-neutral-300 transition-all"
              >
                <div>
                  {/* Card Header */}
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div>
                      <span className={`inline-block px-2.5 py-0.5 rounded-full text-[11px] font-bold border uppercase tracking-wider mb-2 ${def.color}`}>
                        {def.badge}
                      </span>
                      <h3 className="text-base font-bold text-neutral-900">{def.name}</h3>
                      <p className="text-xs text-neutral-500">{ch.accountName}</p>
                    </div>

                    {/* Status Pill */}
                    <div
                      className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
                        isOk
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : isError
                          ? 'bg-rose-50 text-rose-700 border border-rose-200'
                          : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                      }`}
                    >
                      {isOk && <Check className="w-3.5 h-3.5 text-emerald-600" />}
                      {isError && <AlertTriangle className="w-3.5 h-3.5 text-rose-600" />}
                      {!isOk && !isError && <PauseCircle className="w-3.5 h-3.5 text-neutral-500" />}
                      <span>{isOk ? 'Active' : isError ? 'Needs attention' : 'Disabled'}</span>
                    </div>
                  </div>

                  {/* Sync Details */}
                  <div className="bg-neutral-50 rounded-xl p-3 space-y-2 mt-4 text-xs border border-neutral-100">
                    <div className="flex items-center justify-between text-neutral-600">
                      <span>Room mappings:</span>
                      <span className="font-semibold text-neutral-900">
                        {ch.roomMappingsCount > 0 ? `${ch.roomMappingsCount} mapped` : 'Unmapped'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-neutral-600">
                      <span>Rate plan mappings:</span>
                      <span className="font-semibold text-neutral-900">
                        {ch.rateMappingsCount > 0 ? `${ch.rateMappingsCount} mapped` : 'Unmapped'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-neutral-600">
                      <span>Last sync:</span>
                      <span className="font-semibold text-neutral-900">
                        {ch.lastSyncAt ? new Date(ch.lastSyncAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Never synced'}
                      </span>
                    </div>
                    {ch.errorMessage && (
                      <div className="text-rose-600 text-[11px] font-medium pt-1 border-t border-neutral-200">
                        {ch.errorMessage}
                      </div>
                    )}
                  </div>
                </div>

                {/* Card Actions */}
                <div className="mt-6 pt-4 border-t border-neutral-100 flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleSyncNow(ch.id, ch.accountName)}
                      title="Push current availability & rates immediately"
                      className="p-2 text-neutral-600 hover:text-brand-600 hover:bg-neutral-100 rounded-lg transition-colors"
                    >
                      <RefreshCw className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => openMappingModal(ch)}
                      className="px-3 py-1.5 bg-neutral-100 hover:bg-neutral-200 text-neutral-800 text-xs font-semibold rounded-lg transition-colors flex items-center gap-1.5"
                    >
                      <Sliders className="w-3.5 h-3.5" />
                      Match rooms
                    </button>
                    <button
                      onClick={() => openHistoryDrawer(ch)}
                      title="View sync log history"
                      className="p-2 text-neutral-600 hover:text-brand-600 hover:bg-neutral-100 rounded-lg transition-colors"
                    >
                      <History className="w-4 h-4" />
                    </button>
                  </div>

                  <button
                    onClick={() => handleDeleteAccount(ch.id, ch.accountName)}
                    title="Disconnect channel"
                    className="p-2 text-neutral-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Connect Channel Modal */}
      {isConnectModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl border border-neutral-100">
            <div className="flex items-center justify-between pb-4 border-b border-neutral-100">
              <div className="flex items-center gap-2">
                <Globe2 className="w-5 h-5 text-brand-600" />
                <h3 className="text-lg font-bold text-neutral-900">
                  {connectStep === 1 ? 'Select a Channel' : 'Configure Credentials'}
                </h3>
              </div>
              <button
                onClick={() => setIsConnectModalOpen(false)}
                className="text-neutral-400 hover:text-neutral-700 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {connectStep === 1 ? (
              <div className="mt-4 space-y-4">
                <p className="text-xs text-neutral-500">
                  Choose which OTA channel you want to connect to this property:
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-96 overflow-y-auto p-1">
                  {Object.entries(CHANNEL_DEFINITIONS).map(([key, item]) => (
                    <button
                      key={key}
                      onClick={() => {
                        setSelectedChannelType(key);
                        setAccountName(key === 'MOCK' ? 'Demo Mock OTA' : `${item.name} Primary`);
                        setConnectStep(2);
                      }}
                      className={`text-left p-4 rounded-xl border transition-all ${
                        selectedChannelType === key
                          ? 'border-brand-600 bg-brand-50/40 ring-1 ring-brand-600'
                          : 'border-neutral-200 hover:border-neutral-300 hover:bg-neutral-50'
                      }`}
                    >
                      <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold border uppercase tracking-wider mb-2 ${item.color}`}>
                        {item.badge}
                      </span>
                      <h4 className="font-bold text-sm text-neutral-900">{item.name}</h4>
                      <p className="text-xs text-neutral-500 mt-1 line-clamp-2">{item.desc}</p>
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="mt-4 space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Account Name (for your reference)
                  </label>
                  <input
                    type="text"
                    value={accountName}
                    onChange={(e) => setAccountName(e.target.value)}
                    placeholder="e.g. Booking.com Deluxe Account"
                    className="w-full px-3 py-2 text-sm border border-neutral-300 rounded-lg focus:ring-2 focus:ring-brand-500"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-neutral-700 mb-1">
                      API Key / Partner Token
                    </label>
                    <input
                      type="password"
                      value={apiKey}
                      onChange={(e) => setApiKey(e.target.value)}
                      placeholder="Enter partner key"
                      className="w-full px-3 py-2 text-sm border border-neutral-300 rounded-lg focus:ring-2 focus:ring-brand-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-neutral-700 mb-1">
                      Hotel ID / Legal Entity ID
                    </label>
                    <input
                      type="text"
                      value={hotelId}
                      onChange={(e) => setHotelId(e.target.value)}
                      placeholder="e.g. 104928"
                      className="w-full px-3 py-2 text-sm border border-neutral-300 rounded-lg focus:ring-2 focus:ring-brand-500"
                    />
                  </div>
                </div>

                <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 text-xs text-neutral-600 flex items-start gap-2">
                  <ShieldCheck className="w-4 h-4 text-brand-600 flex-shrink-0 mt-0.5" />
                  <span>
                    Credentials are encrypted at rest with AES-256-GCM. We never log or expose raw API secrets.
                  </span>
                </div>

                {testResult && (
                  <div
                    className={`p-3 rounded-xl text-xs font-medium flex items-center gap-2 ${
                      testResult.success
                        ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                        : 'bg-rose-50 text-rose-800 border border-rose-200'
                    }`}
                  >
                    {testResult.success ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                    ) : (
                      <AlertTriangle className="w-4 h-4 text-rose-600 flex-shrink-0" />
                    )}
                    <span>{testResult.message}</span>
                  </div>
                )}

                <div className="pt-4 border-t border-neutral-100 flex items-center justify-between">
                  <button
                    onClick={() => setConnectStep(1)}
                    className="text-xs text-neutral-500 hover:text-neutral-800"
                  >
                    ← Back to channel selection
                  </button>

                  <div className="flex items-center gap-3">
                    <button
                      onClick={handleTestConnection}
                      disabled={testingConnection}
                      className="px-3 py-2 border border-neutral-300 hover:bg-neutral-50 text-neutral-700 text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-colors"
                    >
                      {testingConnection ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <ShieldCheck className="w-3.5 h-3.5" />}
                      Test connection
                    </button>
                    <button
                      onClick={handleCreateAccount}
                      className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white text-xs font-semibold rounded-lg shadow-sm transition-colors"
                    >
                      Save & Connect
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Match Rooms & Rates Modal */}
      {mappingChannel && mappingOptions && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-neutral-100 max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between pb-4 border-b border-neutral-100">
              <div>
                <h3 className="text-lg font-bold text-neutral-900">
                  Match Rooms and Rates
                </h3>
                <p className="text-xs text-neutral-500">
                  Channel: {mappingChannel.accountName} ({mappingChannel.channelType})
                </p>
              </div>
              <button
                onClick={() => setMappingChannel(null)}
                className="text-neutral-400 hover:text-neutral-700 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto py-4 space-y-6">
              {/* Room Mapping Section */}
              <div>
                <h4 className="text-xs font-bold text-neutral-800 uppercase tracking-wider mb-2">
                  Room Types Mapping
                </h4>
                <div className="border border-neutral-200 rounded-xl overflow-hidden text-xs">
                  <table className="w-full text-left">
                    <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-600">
                      <tr>
                        <th className="p-3 font-semibold">Your Room Type</th>
                        <th className="p-3 font-semibold">Map to Channel Room</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {mappingOptions.internalRooms.map((r) => (
                        <tr key={r.id}>
                          <td className="p-3">
                            <span className="font-semibold text-neutral-900">{r.name}</span>
                            <span className="ml-2 font-mono text-[10px] bg-neutral-100 text-neutral-600 px-1.5 py-0.5 rounded">
                              {r.code}
                            </span>
                          </td>
                          <td className="p-3">
                            <select
                              value={selectedRoomMappings[r.id] || ''}
                              onChange={(e) =>
                                setSelectedRoomMappings({
                                  ...selectedRoomMappings,
                                  [r.id]: e.target.value
                                })
                              }
                              className="w-full px-2 py-1.5 border border-neutral-300 rounded-lg bg-white font-medium text-neutral-700 text-xs focus:ring-2 focus:ring-brand-500"
                            >
                              <option value="">-- Do not sync (Unmapped) --</option>
                              {mappingOptions.channelRooms.map((cr) => (
                                <option key={cr.channelRoomId} value={cr.channelRoomId}>
                                  {cr.channelRoomName} ({cr.channelRoomId})
                                </option>
                              ))}
                            </select>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Rate Plans Mapping Section */}
              <div>
                <h4 className="text-xs font-bold text-neutral-800 uppercase tracking-wider mb-2">
                  Price Plans Mapping
                </h4>
                <div className="border border-neutral-200 rounded-xl overflow-hidden text-xs">
                  <table className="w-full text-left">
                    <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-600">
                      <tr>
                        <th className="p-3 font-semibold">Your Price Plan</th>
                        <th className="p-3 font-semibold">Map to Channel Rate</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {mappingOptions.internalRates.map((rp) => (
                        <tr key={rp.id}>
                          <td className="p-3">
                            <span className="font-semibold text-neutral-900">{rp.name}</span>
                            <span className="ml-2 font-mono text-[10px] bg-neutral-100 text-neutral-600 px-1.5 py-0.5 rounded">
                              {rp.code}
                            </span>
                          </td>
                          <td className="p-3">
                            <select
                              value={selectedRateMappings[rp.id] || ''}
                              onChange={(e) =>
                                setSelectedRateMappings({
                                  ...selectedRateMappings,
                                  [rp.id]: e.target.value
                                })
                              }
                              className="w-full px-2 py-1.5 border border-neutral-300 rounded-lg bg-white font-medium text-neutral-700 text-xs focus:ring-2 focus:ring-brand-500"
                            >
                              <option value="">-- Do not sync (Unmapped) --</option>
                              {mappingOptions.channelRates.map((cr) => (
                                <option key={cr.channelRateId} value={cr.channelRateId}>
                                  {cr.channelRateName} ({cr.channelRateId})
                                </option>
                              ))}
                            </select>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div className="pt-4 border-t border-neutral-100 flex items-center justify-between">
              <span className="text-xs text-neutral-500">
                Unmapped rooms will be ignored during synchronization.
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setMappingChannel(null)}
                  className="px-3 py-2 text-xs font-semibold text-neutral-600 hover:text-neutral-900"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSaveMappings}
                  disabled={savingMappings}
                  className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white text-xs font-semibold rounded-lg shadow-sm flex items-center gap-1.5"
                >
                  {savingMappings ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                  Save and Sync
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Sync History Drawer */}
      {historyChannel && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex justify-end">
          <div className="bg-white max-w-lg w-full h-full shadow-2xl p-6 flex flex-col">
            <div className="flex items-center justify-between pb-4 border-b border-neutral-100">
              <div>
                <h3 className="text-base font-bold text-neutral-900 flex items-center gap-2">
                  <History className="w-4 h-4 text-brand-600" />
                  Sync History
                </h3>
                <p className="text-xs text-neutral-500">{historyChannel.accountName}</p>
              </div>
              <button
                onClick={() => setHistoryChannel(null)}
                className="text-neutral-400 hover:text-neutral-700 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto py-4 space-y-3">
              {loadingHistory ? (
                <div className="py-20 flex flex-col items-center justify-center text-neutral-400">
                  <RefreshCw className="w-6 h-6 animate-spin mb-2" />
                  <p className="text-xs">Loading sync jobs...</p>
                </div>
              ) : syncJobs.length === 0 ? (
                <div className="text-center py-16 text-neutral-400 text-xs">
                  No sync attempts recorded for this channel yet.
                </div>
              ) : (
                syncJobs.map((job) => (
                  <div
                    key={job.id}
                    className="p-3.5 rounded-xl border border-neutral-200 bg-neutral-50/50 text-xs space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-neutral-800">{job.jobType.replace('_', ' ')}</span>
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                          job.status === 'SUCCESS'
                            ? 'bg-emerald-100 text-emerald-800'
                            : job.status === 'FAILED'
                            ? 'bg-rose-100 text-rose-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {job.status}
                      </span>
                    </div>

                    <p className="text-neutral-600 text-[11px]">{job.payloadSummary}</p>

                    {job.responseSummary && (
                      <p className="text-emerald-700 text-[11px] font-medium bg-emerald-50 p-2 rounded border border-emerald-100">
                        {job.responseSummary}
                      </p>
                    )}

                    {job.errorMessage && (
                      <div className="text-rose-700 text-[11px] font-medium bg-rose-50 p-2 rounded border border-rose-100 flex items-center justify-between">
                        <span>{job.errorMessage}</span>
                        <button
                          onClick={() => handleRetryJob(job.id)}
                          className="px-2 py-1 bg-white hover:bg-neutral-100 text-rose-700 font-bold rounded shadow-xs border border-rose-200 ml-2"
                        >
                          Retry
                        </button>
                      </div>
                    )}

                    <div className="text-[10px] text-neutral-400 flex items-center justify-between pt-1">
                      <span>{new Date(job.createdAt).toLocaleString()}</span>
                      <span>Attempt {job.attemptCount}/{job.maxAttempts}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
