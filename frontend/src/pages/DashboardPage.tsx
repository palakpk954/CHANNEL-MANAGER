import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Calendar,
  Users,
  TrendingUp,
  Globe2,
  AlertTriangle,
  CheckCircle2,
  ArrowRight,
  Building2,
  RefreshCw,
  Plus,
  BedDouble,
  ChevronRight
} from 'lucide-react';
import api from '../lib/api';

interface Property {
  id: string;
  name: string;
  currency: string;
}

interface DashboardGuestItem {
  reservationId: string;
  guestName: string;
  channelType: string;
  channelReservationId: string;
  roomTypeName: string;
  checkInDate: string;
  checkOutDate: string;
  unitsBooked: number;
  totalAmountCents: number;
  status: string;
}

interface DailyOccupancyTrend {
  date: string;
  totalUnits: number;
  soldUnits: number;
  availableUnits: number;
  occupancyPercentage: number;
}

interface ChannelHealthItem {
  channelAccountId: string;
  channelType: string;
  accountName: string;
  status: string;
  lastSyncAt: string;
  errorMessage: string | null;
  mappedRoomsCount: number;
}

interface DashboardSummary {
  propertyId: string;
  propertyName: string;
  currency: string;
  todayArrivalsCount: number;
  todayDeparturesCount: number;
  inHouseStayoversCount: number;
  occupiedRoomsToday: number;
  totalRooms: number;
  occupancyRatePercentage: number;
  todayRevenueCents: number;
  monthToDateRevenueCents: number;
  unresolvedConflictsCount: number;
  activeChannelsCount: number;
  totalChannelsCount: number;
  todayArrivals: DashboardGuestItem[];
  todayDepartures: DashboardGuestItem[];
  recentBookings: DashboardGuestItem[];
  sevenDayOccupancyTrend: DailyOccupancyTrend[];
  channelHealthList: ChannelHealthItem[];
}

const CHANNEL_BADGES: Record<string, { label: string; badgeClass: string }> = {
  DIRECT: { label: 'Direct Walk-in', badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  MOCK: { label: 'Mock OTA', badgeClass: 'bg-slate-100 text-slate-700 border-slate-200' },
  BOOKING_COM: { label: 'Booking.com', badgeClass: 'bg-blue-50 text-blue-700 border-blue-200' },
  EXPEDIA: { label: 'Expedia', badgeClass: 'bg-amber-50 text-amber-700 border-amber-200' },
  AIRBNB: { label: 'Airbnb', badgeClass: 'bg-rose-50 text-rose-700 border-rose-200' },
  AGODA: { label: 'Agoda', badgeClass: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  MAKEMYTRIP: { label: 'MakeMyTrip', badgeClass: 'bg-red-50 text-red-700 border-red-200' },
  GOIBIBO: { label: 'Goibibo', badgeClass: 'bg-orange-50 text-orange-700 border-orange-200' },
  ICAL: { label: 'iCal Feed', badgeClass: 'bg-purple-50 text-purple-700 border-purple-200' }
};

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'arrivals' | 'departures' | 'recent'>('arrivals');

  useEffect(() => {
    loadProperties();
  }, []);

  useEffect(() => {
    if (selectedPropertyId) {
      loadDashboard(selectedPropertyId);
    }
  }, [selectedPropertyId]);

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

  const loadDashboard = async (propId: string) => {
    try {
      setLoading(true);
      const res = await api.get<DashboardSummary>(`/properties/${propId}/dashboard`);
      setSummary(res.data);
    } catch (err) {
      console.error('Failed to load dashboard', err);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (cents: number, curr: string) => {
    const symbol = curr === 'INR' ? '₹' : curr === 'EUR' ? '€' : '$';
    return `${symbol}${(cents / 100).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
  };

  const getDayName = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { weekday: 'short' });
  };

  return (
    <div className="space-y-6">
      {/* Top Bar: Title & Property Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Hotel Overview</h1>
          <p className="text-slate-500 text-sm mt-0.5">
            Real-time occupancy, guest movements, and channel sync health.
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
            onClick={() => loadDashboard(selectedPropertyId)}
            className="p-2 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 transition-colors shadow-sm"
            title="Refresh dashboard"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          <button
            onClick={() => navigate('/calendar')}
            className="flex items-center gap-2 bg-[#217A4D] hover:bg-[#1b653f] text-white px-4 py-2 rounded-lg font-medium text-sm transition-colors shadow-sm"
          >
            <Calendar className="w-4 h-4" />
            <span>Open Calendar</span>
          </button>
        </div>
      </div>

      {/* Double Booking Critical Alert (Architecture Rule 6) */}
      {summary && summary.unresolvedConflictsCount > 0 && (
        <div className="bg-amber-50 border-2 border-amber-300 rounded-xl p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="p-2 bg-amber-100 rounded-lg text-amber-800 shrink-0">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-semibold text-amber-900 text-base">
                  Action required: {summary.unresolvedConflictsCount} double booking detected
                </h3>
                <p className="text-amber-800 text-sm mt-0.5">
                  Simultaneous bookings occurred. Overbooking protection was triggered. Reassign room or contact guest.
                </p>
              </div>
            </div>
            <button
              onClick={() => navigate('/bookings')}
              className="bg-amber-700 hover:bg-amber-800 text-white px-4 py-2 rounded-lg font-medium text-sm transition-colors whitespace-nowrap shadow-sm"
            >
              Review and resolve
            </button>
          </div>
        </div>
      )}

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Occupancy Rate */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Today's Occupancy</span>
            <BedDouble className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">
              {summary ? `${summary.occupancyRatePercentage}%` : '0%'}
            </span>
            <span className="text-xs text-slate-500">
              ({summary?.occupiedRoomsToday || 0}/{summary?.totalRooms || 0} rooms)
            </span>
          </div>
          <div className="mt-3 w-full bg-slate-100 h-2 rounded-full overflow-hidden">
            <div
              className="bg-[#217A4D] h-full rounded-full transition-all duration-500"
              style={{ width: `${Math.min(100, summary?.occupancyRatePercentage || 0)}%` }}
            />
          </div>
          <div className="mt-2 text-[11px] text-slate-400">
            {summary ? summary.totalRooms - summary.occupiedRoomsToday : 0} rooms open for sale
          </div>
        </div>

        {/* KPI 2: Guest Movements */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Today's Movements</span>
            <Users className="w-4 h-4 text-blue-600" />
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2">
            <div>
              <span className="text-2xl font-bold text-slate-900">{summary?.todayArrivalsCount || 0}</span>
              <span className="block text-[11px] text-emerald-700 font-medium">Arrivals today</span>
            </div>
            <div>
              <span className="text-2xl font-bold text-slate-900">{summary?.todayDeparturesCount || 0}</span>
              <span className="block text-[11px] text-slate-500 font-medium">Departures today</span>
            </div>
          </div>
          <div className="mt-3 pt-2.5 border-t border-slate-100 text-[11px] text-slate-500">
            In-house stayovers: <span className="font-semibold text-slate-700">{summary?.inHouseStayoversCount || 0}</span>
          </div>
        </div>

        {/* KPI 3: Revenue (MTD & Today) */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Month-to-Date Revenue</span>
            <TrendingUp className="w-4 h-4 text-indigo-600" />
          </div>
          <div className="mt-3">
            <span className="text-2xl font-extrabold text-slate-900">
              {summary ? formatCurrency(summary.monthToDateRevenueCents, summary.currency) : '₹0'}
            </span>
          </div>
          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
            <span>Today's arriving revenue:</span>
            <span className="font-semibold text-slate-800">
              {summary ? formatCurrency(summary.todayRevenueCents, summary.currency) : '₹0'}
            </span>
          </div>
        </div>

        {/* KPI 4: Channels Connected */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 text-xs font-semibold uppercase tracking-wider">
            <span>Connected OTAs</span>
            <Globe2 className="w-4 h-4 text-amber-600" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900">
              {summary ? summary.activeChannelsCount : 0}
            </span>
            <span className="text-xs text-slate-500">
              / {summary ? summary.totalChannelsCount : 0} configured
            </span>
          </div>
          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between">
            <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-emerald-700">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              Sync healthy
            </span>
            <button
              onClick={() => navigate('/channels/health')}
              className="text-indigo-600 hover:text-indigo-800 text-[11px] font-medium flex items-center gap-0.5"
            >
              <span>Channel health</span>
              <ChevronRight className="w-3 h-3" />
            </button>
          </div>
        </div>
      </div>

      {/* 7-Day Occupancy Outlook & Channel Status Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 7-Day Occupancy Bar Forecast */}
        <div className="lg:col-span-2 bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-bold text-slate-900 text-base">7-Day Occupancy Outlook</h3>
              <p className="text-xs text-slate-500 mt-0.5">Projected room demand across all active channels.</p>
            </div>
            <button
              onClick={() => navigate('/calendar')}
              className="text-xs font-semibold text-emerald-700 hover:text-emerald-800 flex items-center gap-1"
            >
              <span>Full calendar</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-2 pt-2">
            {summary?.sevenDayOccupancyTrend.map((day, idx) => {
              const dayName = getDayName(day.date);
              const isToday = idx === 0;

              return (
                <div
                  key={day.date}
                  className={`p-3 rounded-xl border text-center transition-all ${
                    isToday
                      ? 'border-emerald-600 bg-emerald-50/40 ring-1 ring-emerald-500'
                      : 'border-slate-200 bg-slate-50/50'
                  }`}
                >
                  <div className="text-[11px] font-bold text-slate-500 uppercase">{dayName}</div>
                  <div className="text-xs font-semibold text-slate-800 mt-0.5">
                    {day.date.split('-').slice(1).join('/')}
                  </div>

                  <div className="my-3 flex flex-col items-center">
                    <span className="text-lg font-extrabold text-slate-900">{day.occupancyPercentage}%</span>
                    <div className="w-full bg-slate-200 h-1.5 rounded-full mt-1.5 overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          day.occupancyPercentage > 80
                            ? 'bg-rose-500'
                            : day.occupancyPercentage > 50
                            ? 'bg-amber-500'
                            : 'bg-emerald-600'
                        }`}
                        style={{ width: `${day.occupancyPercentage}%` }}
                      />
                    </div>
                  </div>

                  <div className="text-[10px] text-slate-500">
                    <span className="font-semibold text-slate-700">{day.soldUnits}</span> booked
                  </div>
                  <div className="text-[10px] text-emerald-700 font-medium">
                    <span>{day.availableUnits}</span> left
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Channel Health Status Quick Card */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-slate-900 text-base">Channel Connections</h3>
              <span className="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded-full">
                Live Sync
              </span>
            </div>
            <p className="text-xs text-slate-500 mb-4">
              Real-time synchronization status with online travel agents.
            </p>

            <div className="space-y-3">
              {summary?.channelHealthList && summary.channelHealthList.length > 0 ? (
                summary.channelHealthList.map((ch) => {
                  const badge = CHANNEL_BADGES[ch.channelType] || {
                    label: ch.channelType,
                    badgeClass: 'bg-slate-100 text-slate-700'
                  };

                  return (
                    <div
                      key={ch.channelAccountId}
                      className="p-3 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-2.5">
                        <span
                          className={`w-2.5 h-2.5 rounded-full shrink-0 ${
                            ch.status === 'CONNECTED'
                              ? 'bg-emerald-500'
                              : ch.status === 'ERROR'
                              ? 'bg-rose-500 animate-pulse'
                              : 'bg-amber-500'
                          }`}
                        />
                        <div>
                          <div className="font-semibold text-slate-800">{badge.label}</div>
                          <div className="text-[10px] text-slate-400 font-mono">
                            {ch.mappedRoomsCount} room(s) mapped
                          </div>
                        </div>
                      </div>

                      <div className="text-right">
                        <span
                          className={`inline-block px-2 py-0.5 rounded text-[10px] font-semibold border ${
                            ch.status === 'CONNECTED'
                              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                              : 'bg-rose-50 text-rose-700 border-rose-200'
                          }`}
                        >
                          {ch.status === 'CONNECTED' ? 'Synced' : 'Error'}
                        </span>
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="text-center py-6 text-slate-400 text-xs">
                  No channel accounts configured yet.
                </div>
              )}
            </div>
          </div>

          <div className="pt-4 mt-4 border-t border-slate-100">
            <button
              onClick={() => navigate('/channels')}
              className="w-full py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 font-semibold text-xs rounded-lg border border-slate-200 transition-colors flex items-center justify-center gap-1.5"
            >
              <span>Connect new OTA channel</span>
              <Plus className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Movements Table (Arrivals, Departures, Recent) */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {/* Table Tabs */}
        <div className="border-b border-slate-200 px-6 pt-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setActiveTab('arrivals')}
              className={`pb-3 text-xs font-semibold border-b-2 transition-colors ${
                activeTab === 'arrivals'
                  ? 'border-emerald-600 text-emerald-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              Today's Arrivals ({summary?.todayArrivalsCount || 0})
            </button>
            <button
              onClick={() => setActiveTab('departures')}
              className={`pb-3 text-xs font-semibold border-b-2 transition-colors ${
                activeTab === 'departures'
                  ? 'border-emerald-600 text-emerald-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              Today's Departures ({summary?.todayDeparturesCount || 0})
            </button>
            <button
              onClick={() => setActiveTab('recent')}
              className={`pb-3 text-xs font-semibold border-b-2 transition-colors ${
                activeTab === 'recent'
                  ? 'border-emerald-600 text-emerald-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              Recent Bookings
            </button>
          </div>

          <button
            onClick={() => navigate('/bookings')}
            className="text-xs font-semibold text-emerald-700 hover:text-emerald-800 flex items-center gap-1 pb-3"
          >
            <span>View all in bookings inbox</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Table Content */}
        <div className="overflow-x-auto">
          {(() => {
            if (loading) {
              return (
                <div className="p-12 text-center text-slate-400 text-xs">
                  <RefreshCw className="w-8 h-8 text-slate-300 mx-auto mb-2 animate-spin" />
                  <p>Loading movements and bookings...</p>
                </div>
              );
            }

            const currentList =
              activeTab === 'arrivals'
                ? summary?.todayArrivals || []
                : activeTab === 'departures'
                ? summary?.todayDepartures || []
                : summary?.recentBookings || [];

            if (currentList.length === 0) {
              return (
                <div className="p-12 text-center text-slate-400 text-xs">
                  <BedDouble className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                  <p>No bookings in this view today.</p>
                </div>
              );
            }

            return (
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[11px]">
                    <th className="py-3 px-6">Guest</th>
                    <th className="py-3 px-6">Channel</th>
                    <th className="py-3 px-6">Room Type</th>
                    <th className="py-3 px-6">Stay Dates</th>
                    <th className="py-3 px-6">Amount</th>
                    <th className="py-3 px-6">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {currentList.map((item) => {
                    const badge = CHANNEL_BADGES[item.channelType] || {
                      label: item.channelType,
                      badgeClass: 'bg-slate-100 text-slate-700'
                    };

                    return (
                      <tr
                        key={item.reservationId}
                        onClick={() => navigate('/bookings')}
                        className="hover:bg-slate-50 cursor-pointer transition-colors"
                      >
                        <td className="py-3.5 px-6 font-semibold text-slate-900">
                          {item.guestName}
                        </td>
                        <td className="py-3.5 px-6">
                          <span
                            className={`inline-block px-2.5 py-0.5 rounded-full font-medium text-[11px] border ${badge.badgeClass}`}
                          >
                            {badge.label}
                          </span>
                          <span className="block text-slate-400 font-mono text-[10px] mt-0.5">
                            #{item.channelReservationId}
                          </span>
                        </td>
                        <td className="py-3.5 px-6 font-medium text-slate-700">
                          {item.roomTypeName} ({item.unitsBooked} unit{item.unitsBooked > 1 ? 's' : ''})
                        </td>
                        <td className="py-3.5 px-6 text-slate-700">
                          {item.checkInDate} <span className="text-slate-400">→</span> {item.checkOutDate}
                        </td>
                        <td className="py-3.5 px-6 font-bold text-slate-900">
                          {formatCurrency(item.totalAmountCents, summary?.currency || 'INR')}
                        </td>
                        <td className="py-3.5 px-6">
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                            Confirmed
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            );
          })()}
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
