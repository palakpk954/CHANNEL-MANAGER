import React, { useState, useEffect } from 'react';
import { apiRequest } from '../lib/api';
import { StatusChip } from '../components/common/StatusChip';
import {
  BedDouble,
  Tag,
  Plus,
  Users,
  FileSpreadsheet,
  X
} from 'lucide-react';

interface RoomType {
  id: string;
  code: string;
  name: string;
  maxOccupancy: number;
  totalUnits: number;
  bedType: string;
  amenities: string;
  status: string;
}

interface RatePlan {
  id: string;
  roomTypeId: string;
  code: string;
  name: string;
  mealPlan: string;
  cancellationPolicy: string;
  derivationType?: string;
  derivationValue?: number;
  status: string;
}

interface Property {
  id: string;
  name: string;
  currency: string;
  timezone: string;
}

export const RoomsAndPricesPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'rooms' | 'prices'>('rooms');
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [ratePlans, setRatePlans] = useState<RatePlan[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // Modals state
  const [showAddRoom, setShowAddRoom] = useState<boolean>(false);
  const [showAddRate, setShowAddRate] = useState<boolean>(false);
  const [showCsvModal, setShowCsvModal] = useState<boolean>(false);

  // Form states
  const [roomCode, setRoomCode] = useState('');
  const [roomName, setRoomName] = useState('');
  const [maxOccupancy, setMaxOccupancy] = useState(2);
  const [totalUnits, setTotalUnits] = useState(5);
  const [bedType, setBedType] = useState('Queen Bed');

  const [rateCode, setRateCode] = useState('');
  const [rateName, setRateName] = useState('');
  const [linkedRoomId, setLinkedRoomId] = useState('');
  const [mealPlan, setMealPlan] = useState('ROOM_ONLY');
  const [cancellationPolicy, setCancellationPolicy] = useState('FREE_CANCELLATION_24H');

  const [csvContent, setCsvContent] = useState('');
  const [csvResult, setCsvResult] = useState<string | null>(null);

  useEffect(() => {
    loadProperties();
  }, []);

  useEffect(() => {
    if (selectedPropertyId) {
      loadData(selectedPropertyId);
    }
  }, [selectedPropertyId]);

  const loadProperties = async () => {
    try {
      const data = await apiRequest<Property[]>('/properties');
      setProperties(data);
      if (data.length > 0) {
        setSelectedPropertyId(data[0].id);
      }
    } catch {
      // Handled by apiRequest
    } finally {
      setLoading(false);
    }
  };

  const loadData = async (propId: string) => {
    try {
      setLoading(true);
      const [rooms, rates] = await Promise.all([
        apiRequest<RoomType[]>(`/properties/${propId}/room-types`),
        apiRequest<RatePlan[]>(`/properties/${propId}/rate-plans`),
      ]);
      setRoomTypes(rooms);
      setRatePlans(rates);
      if (rooms.length > 0 && !linkedRoomId) {
        setLinkedRoomId(rooms[0].id);
      }
    } catch {
      // Handled
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiRequest(`/properties/${selectedPropertyId}/room-types`, {
        method: 'POST',
        body: JSON.stringify({
          code: roomCode,
          name: roomName,
          maxOccupancy,
          totalUnits,
          bedType,
        }),
      });
      setShowAddRoom(false);
      setRoomCode('');
      setRoomName('');
      loadData(selectedPropertyId);
    } catch {
      // Handled
    }
  };

  const handleCreateRate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiRequest(`/properties/${selectedPropertyId}/rate-plans`, {
        method: 'POST',
        body: JSON.stringify({
          code: rateCode,
          name: rateName,
          roomTypeId: linkedRoomId,
          mealPlan,
          cancellationPolicy,
        }),
      });
      setShowAddRate(false);
      setRateCode('');
      setRateName('');
      loadData(selectedPropertyId);
    } catch {
      // Handled
    }
  };

  const handleCsvImport = async () => {
    try {
      const res = await apiRequest<{ message: string }>(`/properties/${selectedPropertyId}/import-csv-text`, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: csvContent,
      });
      setCsvResult(res.message);
      loadData(selectedPropertyId);
    } catch (err: any) {
      setCsvResult(err.message || 'Import failed. Please check the row formatting.');
    }
  };

  return (
    <div className="space-y-6 max-w-6xl">
      {/* Header with Title & Action Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-lg border border-neutral-300">
        <div>
          <h1 className="text-xl font-semibold text-neutral-900">Rooms and prices</h1>
          <p className="text-xs text-neutral-600 mt-1">
            Configure room types, physical units, and sellable price plans across all channels.
          </p>
          {properties.length > 1 && (
            <div className="mt-2 flex items-center gap-2">
              <span className="text-xs text-neutral-500">Property:</span>
              <select
                value={selectedPropertyId}
                onChange={(e) => setSelectedPropertyId(e.target.value)}
                className="text-xs border border-neutral-300 rounded px-2 py-1 bg-white"
              >
                {properties.map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
          )}
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowCsvModal(true)}
            className="px-3 py-2 bg-cream-50 hover:bg-cream-100 border border-neutral-300 text-neutral-900 text-xs font-medium rounded-lg flex items-center gap-1.5 transition-colors"
          >
            <FileSpreadsheet className="w-4 h-4 text-brand-600" />
            <span>Import CSV</span>
          </button>

          {activeTab === 'rooms' ? (
            <button
              onClick={() => setShowAddRoom(true)}
              className="px-3.5 py-2 bg-brand-600 hover:bg-brand-700 text-white text-xs font-medium rounded-lg flex items-center gap-1.5 transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>Add a room type</span>
            </button>
          ) : (
            <button
              onClick={() => setShowAddRate(true)}
              className="px-3.5 py-2 bg-brand-600 hover:bg-brand-700 text-white text-xs font-medium rounded-lg flex items-center gap-1.5 transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>Add a price plan</span>
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-neutral-300 gap-6 text-sm font-medium">
        <button
          onClick={() => setActiveTab('rooms')}
          className={`pb-3 flex items-center gap-2 border-b-2 transition-colors ${
            activeTab === 'rooms'
              ? 'border-brand-600 text-brand-600 font-semibold'
              : 'border-transparent text-neutral-600 hover:text-neutral-900'
          }`}
        >
          <BedDouble className="w-4 h-4" />
          <span>Room Types ({roomTypes.length})</span>
        </button>
        <button
          onClick={() => setActiveTab('prices')}
          className={`pb-3 flex items-center gap-2 border-b-2 transition-colors ${
            activeTab === 'prices'
              ? 'border-brand-600 text-brand-600 font-semibold'
              : 'border-transparent text-neutral-600 hover:text-neutral-900'
          }`}
        >
          <Tag className="w-4 h-4" />
          <span>Price Plans ({ratePlans.length})</span>
        </button>
      </div>

      {/* Table Content */}
      {loading ? (
        <div className="bg-white p-12 text-center rounded-lg border border-neutral-300">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-600 mx-auto"></div>
          <p className="text-xs text-neutral-500 mt-3">Loading rooms and prices...</p>
        </div>
      ) : activeTab === 'rooms' ? (
        <div className="bg-white rounded-lg border border-neutral-300 overflow-hidden shadow-sm">
          <table className="w-full text-left border-collapse text-xs">
            <thead className="bg-neutral-100 text-neutral-700 font-semibold border-b border-neutral-300">
              <tr>
                <th className="py-3 px-4">Code</th>
                <th className="py-3 px-4">Room name</th>
                <th className="py-3 px-4">Max guests</th>
                <th className="py-3 px-4">Total rooms</th>
                <th className="py-3 px-4">Bed configuration</th>
                <th className="py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {roomTypes.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-neutral-500">
                    No room types found. Click "Add a room type" to begin.
                  </td>
                </tr>
              ) : (
                roomTypes.map((room) => (
                  <tr key={room.id} className="hover:bg-cream-50/50 transition-colors">
                    <td className="py-3 px-4 font-mono font-medium text-neutral-900">{room.code}</td>
                    <td className="py-3 px-4 font-medium text-neutral-900">{room.name}</td>
                    <td className="py-3 px-4 text-neutral-600 flex items-center gap-1">
                      <Users className="w-3.5 h-3.5 text-neutral-400" />
                      <span>{room.maxOccupancy} guests</span>
                    </td>
                    <td className="py-3 px-4 font-mono font-medium text-neutral-900">
                      {room.totalUnits} rooms
                    </td>
                    <td className="py-3 px-4 text-neutral-600">{room.bedType || 'Standard'}</td>
                    <td className="py-3 px-4">
                      <StatusChip status={room.status === 'ACTIVE' ? 'available' : 'closed'} label={room.status} />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="bg-white rounded-lg border border-neutral-300 overflow-hidden shadow-sm">
          <table className="w-full text-left border-collapse text-xs">
            <thead className="bg-neutral-100 text-neutral-700 font-semibold border-b border-neutral-300">
              <tr>
                <th className="py-3 px-4">Code</th>
                <th className="py-3 px-4">Price plan name</th>
                <th className="py-3 px-4">Room type</th>
                <th className="py-3 px-4">Meal plan</th>
                <th className="py-3 px-4">Cancellation policy</th>
                <th className="py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {ratePlans.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-neutral-500">
                    No price plans defined yet. Click "Add a price plan" to create one.
                  </td>
                </tr>
              ) : (
                ratePlans.map((rate) => {
                  const linkedRoom = roomTypes.find((r) => r.id === rate.roomTypeId);
                  return (
                    <tr key={rate.id} className="hover:bg-cream-50/50 transition-colors">
                      <td className="py-3 px-4 font-mono font-medium text-neutral-900">{rate.code}</td>
                      <td className="py-3 px-4 font-medium text-neutral-900">{rate.name}</td>
                      <td className="py-3 px-4 text-neutral-700">{linkedRoom?.name || rate.roomTypeId}</td>
                      <td className="py-3 px-4 text-neutral-600">{rate.mealPlan.replace('_', ' ')}</td>
                      <td className="py-3 px-4 text-neutral-600">{rate.cancellationPolicy.replace(/_/g, ' ')}</td>
                      <td className="py-3 px-4">
                        <StatusChip status="available" label="Active" />
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Add Room Modal */}
      {showAddRoom && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg border border-neutral-300 shadow-xl max-w-md w-full p-6">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-200">
              <h2 className="text-base font-semibold text-neutral-900">Add a room type</h2>
              <button onClick={() => setShowAddRoom(false)} className="text-neutral-400 hover:text-neutral-700">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleCreateRoom} className="space-y-4 mt-4 text-xs">
              <div>
                <label className="block font-medium text-neutral-900 mb-1">Room code</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. DLX"
                  value={roomCode}
                  onChange={(e) => setRoomCode(e.target.value)}
                  className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                />
              </div>
              <div>
                <label className="block font-medium text-neutral-900 mb-1">Room name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Deluxe King Suite"
                  value={roomName}
                  onChange={(e) => setRoomName(e.target.value)}
                  className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-neutral-900 mb-1">Max guests</label>
                  <input
                    type="number"
                    min={1}
                    required
                    value={maxOccupancy}
                    onChange={(e) => setMaxOccupancy(parseInt(e.target.value) || 1)}
                    className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                  />
                </div>
                <div>
                  <label className="block font-medium text-neutral-900 mb-1">Total physical units</label>
                  <input
                    type="number"
                    min={1}
                    required
                    value={totalUnits}
                    onChange={(e) => setTotalUnits(parseInt(e.target.value) || 1)}
                    className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                  />
                </div>
              </div>
              <div>
                <label className="block font-medium text-neutral-900 mb-1">Bed configuration</label>
                <input
                  type="text"
                  placeholder="e.g. 1 King bed or 2 Twin beds"
                  value={bedType}
                  onChange={(e) => setBedType(e.target.value)}
                  className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                />
              </div>
              <div className="flex justify-end gap-2 pt-4 border-t border-neutral-200">
                <button
                  type="button"
                  onClick={() => setShowAddRoom(false)}
                  className="px-3.5 py-2 border border-neutral-300 rounded-lg text-neutral-700 hover:bg-neutral-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-3.5 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-lg font-medium"
                >
                  Save room type
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Rate Modal */}
      {showAddRate && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg border border-neutral-300 shadow-xl max-w-md w-full p-6">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-200">
              <h2 className="text-base font-semibold text-neutral-900">Add a price plan</h2>
              <button onClick={() => setShowAddRate(false)} className="text-neutral-400 hover:text-neutral-700">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleCreateRate} className="space-y-4 mt-4 text-xs">
              <div>
                <label className="block font-medium text-neutral-900 mb-1">Price plan code</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. BAR_RO"
                  value={rateCode}
                  onChange={(e) => setRateCode(e.target.value)}
                  className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                />
              </div>
              <div>
                <label className="block font-medium text-neutral-900 mb-1">Price plan name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Best Available Rate (Room Only)"
                  value={rateName}
                  onChange={(e) => setRateName(e.target.value)}
                  className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                />
              </div>
              <div>
                <label className="block font-medium text-neutral-900 mb-1">Linked room type</label>
                <select
                  value={linkedRoomId}
                  onChange={(e) => setLinkedRoomId(e.target.value)}
                  className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                >
                  {roomTypes.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name} ({r.code})
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-neutral-900 mb-1">Meal plan</label>
                  <select
                    value={mealPlan}
                    onChange={(e) => setMealPlan(e.target.value)}
                    className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                  >
                    <option value="ROOM_ONLY">Room only</option>
                    <option value="BED_AND_BREAKFAST">Bed and breakfast</option>
                    <option value="HALF_BOARD">Half board</option>
                    <option value="FULL_BOARD">Full board</option>
                  </select>
                </div>
                <div>
                  <label className="block font-medium text-neutral-900 mb-1">Cancellation policy</label>
                  <select
                    value={cancellationPolicy}
                    onChange={(e) => setCancellationPolicy(e.target.value)}
                    className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-600"
                  >
                    <option value="FREE_CANCELLATION_24H">Free cancellation 24h</option>
                    <option value="NON_REFUNDABLE">Non-refundable</option>
                  </select>
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-4 border-t border-neutral-200">
                <button
                  type="button"
                  onClick={() => setShowAddRate(false)}
                  className="px-3.5 py-2 border border-neutral-300 rounded-lg text-neutral-700 hover:bg-neutral-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-3.5 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-lg font-medium"
                >
                  Save price plan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CSV Import Modal */}
      {showCsvModal && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg border border-neutral-300 shadow-xl max-w-lg w-full p-6">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-200">
              <h2 className="text-base font-semibold text-neutral-900">Import rooms and prices from CSV</h2>
              <button onClick={() => setShowCsvModal(false)} className="text-neutral-400 hover:text-neutral-700">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="mt-4 space-y-3 text-xs">
              <p className="text-neutral-600">
                Paste CSV data below with lines formatted as:
              </p>
              <pre className="bg-neutral-100 p-2.5 rounded font-mono text-[11px] text-neutral-800 overflow-x-auto">
{`type,code,name,max_occupancy,total_units,bed_type,amenities
ROOM,DLX,Deluxe Room,2,10,King,WiFi
ROOM,STE,Presidential Suite,4,2,King,WiFi
RATE,BAR,Standard Rate,DLX,ROOM_ONLY,FREE_24H
RATE,BB,Bed & Breakfast,DLX,BED_AND_BREAKFAST,NON_REFUNDABLE`}
              </pre>
              <textarea
                rows={6}
                value={csvContent}
                onChange={(e) => setCsvContent(e.target.value)}
                placeholder="Paste CSV rows here..."
                className="w-full px-3 py-2 border border-neutral-400 rounded-lg font-mono text-xs focus:outline-none focus:ring-2 focus:ring-brand-600"
              />
              {csvResult && (
                <div className="p-3 bg-cream-100 rounded border border-cream-200 text-neutral-900 text-xs">
                  {csvResult}
                </div>
              )}
            </div>
            <div className="flex justify-end gap-2 pt-4 border-t border-neutral-200 mt-4">
              <button
                type="button"
                onClick={() => setShowCsvModal(false)}
                className="px-3.5 py-2 border border-neutral-300 rounded-lg text-neutral-700 hover:bg-neutral-100 text-xs"
              >
                Close
              </button>
              <button
                type="button"
                onClick={handleCsvImport}
                className="px-3.5 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-lg font-medium text-xs"
              >
                Process CSV import
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
