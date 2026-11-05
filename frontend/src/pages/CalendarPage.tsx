import React, { useState, useEffect } from 'react';
import { apiRequest } from '../lib/api';
import {
  ChevronLeft,
  ChevronRight,
  SlidersHorizontal,
  Lock,
  ArrowRight,
  RotateCcw,
  Check,
  X
} from 'lucide-react';

interface CalendarGridResponse {
  propertyId: string;
  propertyName: string;
  currency: string;
  timezone: string;
  startDate: string;
  endDate: string;
  dates: string[];
  rooms: RoomRow[];
  ratePlans: RatePlanRow[];
}

interface RoomRow {
  id: string;
  code: string;
  name: string;
  totalUnits: number;
  days: Record<string, { total: number; sold: number; blocked: number; available: number; statusToken: string }>;
}

interface RatePlanRow {
  id: string;
  roomTypeId: string;
  code: string;
  name: string;
  days: Record<string, { amountCents: number; amountFormatted: number; currency: string; stopSell: boolean; cta: boolean; ctd: boolean; minStay: number; maxStay?: number; hasRestrictions: boolean }>;
}

interface Property {
  id: string;
  name: string;
}

export const CalendarPage: React.FC = () => {
  const [properties, setProperties] = useState<Property[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [calendarData, setCalendarData] = useState<CalendarGridResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [viewMode, setViewMode] = useState<'both' | 'rooms' | 'prices'>('both');
  const [currentStartDate, setCurrentStartDate] = useState<string>(new Date().toISOString().split('T')[0]);

  // Modals & Side Panels
  const [selectedDayCell, setSelectedDayCell] = useState<{ date: string; room?: RoomRow; ratePlan?: RatePlanRow } | null>(null);
  const [showBulkPanel, setShowBulkPanel] = useState<boolean>(false);
  const [showHoldModal, setShowHoldModal] = useState<boolean>(false);
  const [bulkStep, setBulkStep] = useState<number>(1);

  // Edit Day state
  const [editPrice, setEditPrice] = useState<number>(4500);
  const [editStopSell, setEditStopSell] = useState<boolean>(false);
  const [editMinStay, setEditMinStay] = useState<number>(1);
  const [editCta, setEditCta] = useState<boolean>(false);
  const [editCtd, setEditCtd] = useState<boolean>(false);
  const [editAvailable, setEditAvailable] = useState<number>(5);

  // Bulk update state
  const [bulkStart, setBulkStart] = useState<string>(new Date().toISOString().split('T')[0]);
  const [bulkEnd, setBulkEnd] = useState<string>(new Date(Date.now() + 14 * 86400000).toISOString().split('T')[0]);
  const [selectedWeekdays, setSelectedWeekdays] = useState<number[]>([1, 2, 3, 4, 5, 6, 7]);
  const [selectedBulkRooms, setSelectedBulkRooms] = useState<string[]>([]);
  const [selectedBulkRates, setSelectedBulkRates] = useState<string[]>([]);
  const [bulkPriceValue, setBulkPriceValue] = useState<number>(4500);
  const [bulkStopSell, setBulkStopSell] = useState<boolean>(false);
  const [bulkMinStay, setBulkMinStay] = useState<number>(1);
  const [confirmLargePriceChange, setConfirmLargePriceChange] = useState<boolean>(false);

  // Undo Toast state
  const [lastHistoryId, setLastHistoryId] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    loadProperties();
  }, []);

  useEffect(() => {
    if (selectedPropertyId) {
      loadCalendar(selectedPropertyId, currentStartDate);
    }
  }, [selectedPropertyId, currentStartDate]);

  // Keyboard shortcuts (Appendix A / Design System 5.2: B for Bulk, H for Hold)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if (e.key === 'b' || e.key === 'B') {
        setShowBulkPanel(true);
      } else if (e.key === 'h' || e.key === 'H') {
        setShowHoldModal(true);
      } else if (e.key === 'Escape') {
        setSelectedDayCell(null);
        setShowBulkPanel(false);
        setShowHoldModal(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const loadProperties = async () => {
    try {
      const data = await apiRequest<Property[]>('/properties');
      setProperties(data);
      if (data.length > 0) {
        setSelectedPropertyId(data[0].id);
      }
    } catch {
      // Handled
    }
  };

  const loadCalendar = async (propId: string, startDateStr: string) => {
    try {
      setLoading(true);
      const start = new Date(startDateStr);
      const end = new Date(start.getTime() + 14 * 86400000); // 14-day window default
      const res = await apiRequest<CalendarGridResponse>(
        `/properties/${propId}/calendar?from=${startDateStr}&to=${end.toISOString().split('T')[0]}`
      );
      setCalendarData(res);
      if (res.rooms.length > 0 && selectedBulkRooms.length === 0) {
        setSelectedBulkRooms([res.rooms[0].id]);
      }
      if (res.ratePlans.length > 0 && selectedBulkRates.length === 0) {
        setSelectedBulkRates([res.ratePlans[0].id]);
      }
    } catch {
      // Handled
    } finally {
      setLoading(false);
    }
  };

  const handlePrevDays = () => {
    const cur = new Date(currentStartDate);
    cur.setDate(cur.getDate() - 7);
    setCurrentStartDate(cur.toISOString().split('T')[0]);
  };

  const handleNextDays = () => {
    const cur = new Date(currentStartDate);
    cur.setDate(cur.getDate() + 7);
    setCurrentStartDate(cur.toISOString().split('T')[0]);
  };

  const handleOpenEditDay = (date: string, room?: RoomRow, ratePlan?: RatePlanRow) => {
    setSelectedDayCell({ date, room, ratePlan });
    if (room && room.days[date]) {
      setEditAvailable(room.days[date].available);
    }
    if (ratePlan && ratePlan.days[date]) {
      setEditPrice(ratePlan.days[date].amountFormatted);
      setEditStopSell(ratePlan.days[date].stopSell);
      setEditMinStay(ratePlan.days[date].minStay);
      setEditCta(ratePlan.days[date].cta);
      setEditCtd(ratePlan.days[date].ctd);
    }
  };

  const handleSaveDay = async () => {
    if (!selectedDayCell) return;
    try {
      await apiRequest(`/properties/${selectedPropertyId}/calendar/day`, {
        method: 'PUT',
        body: JSON.stringify({
          date: selectedDayCell.date,
          roomTypeId: selectedDayCell.room?.id,
          availableUnits: editAvailable,
          ratePlanId: selectedDayCell.ratePlan?.id,
          amountCents: Math.round(editPrice * 100),
          stopSell: editStopSell,
          minStay: editMinStay,
          cta: editCta,
          ctd: editCtd,
        }),
      });
      setSelectedDayCell(null);
      setToastMessage('Saved. The updated prices are being sent to Booking.com and Agoda.');
      loadCalendar(selectedPropertyId, currentStartDate);
    } catch (err: any) {
      alert(err.message || 'Failed to update day.');
    }
  };

  const handleApplyBulk = async () => {
    try {
      const res = await apiRequest<{ historyId: string; message: string }>(
        `/properties/${selectedPropertyId}/calendar/bulk`,
        {
          method: 'POST',
          body: JSON.stringify({
            startDate: bulkStart,
            endDate: bulkEnd,
            daysOfWeek: selectedWeekdays,
            roomTypeIds: selectedBulkRooms,
            ratePlanIds: selectedBulkRates,
            priceAdjustmentType: 'SET_EXACT',
            priceValue: bulkPriceValue,
            confirmLargePriceChange,
            stopSell: bulkStopSell,
            minStay: bulkMinStay,
          }),
        }
      );
      setShowBulkPanel(false);
      setLastHistoryId(res.historyId);
      setToastMessage('Saved. The updated prices are being sent to Booking.com and Agoda.');
      loadCalendar(selectedPropertyId, currentStartDate);
    } catch (err: any) {
      alert(err.message || 'Failed to apply bulk update.');
    }
  };

  const handleUndo = async () => {
    if (!lastHistoryId) return;
    try {
      const res = await apiRequest<{ message: string }>(
        `/properties/${selectedPropertyId}/calendar/undo/${lastHistoryId}`,
        { method: 'POST' }
      );
      setToastMessage(res.message);
      setLastHistoryId(null);
      loadCalendar(selectedPropertyId, currentStartDate);
    } catch (err: any) {
      alert(err.message || 'Unable to undo change.');
    }
  };

  return (
    <div className="space-y-4 max-w-7xl">
      {/* Top Controls Bar */}
      <div className="bg-white p-4 rounded-lg border border-neutral-300 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          {properties.length > 1 && (
            <select
              value={selectedPropertyId}
              onChange={(e) => setSelectedPropertyId(e.target.value)}
              className="px-2.5 py-1.5 border border-neutral-300 rounded-lg text-xs bg-white font-medium text-neutral-800 focus:ring-2 focus:ring-brand-500"
            >
              {properties.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          )}

          <div className="flex items-center border border-neutral-300 rounded-lg overflow-hidden">
            <button
              onClick={handlePrevDays}
              className="p-1.5 hover:bg-cream-100 text-neutral-700 border-r border-neutral-300 transition-colors"
              title="Previous 7 days"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-3 py-1.5 text-xs font-semibold text-neutral-900 bg-cream-50">
              {calendarData?.startDate} to {calendarData?.endDate}
            </span>
            <button
              onClick={handleNextDays}
              className="p-1.5 hover:bg-cream-100 text-neutral-700 border-l border-neutral-300 transition-colors"
              title="Next 7 days"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* View mode toggle */}
          <div className="flex items-center bg-neutral-100 p-0.5 rounded-lg text-xs font-medium">
            <button
              onClick={() => setViewMode('both')}
              className={`px-2.5 py-1 rounded ${viewMode === 'both' ? 'bg-white shadow-xs font-semibold text-brand-700' : 'text-neutral-600'}`}
            >
              All
            </button>
            <button
              onClick={() => setViewMode('rooms')}
              className={`px-2.5 py-1 rounded ${viewMode === 'rooms' ? 'bg-white shadow-xs font-semibold text-brand-700' : 'text-neutral-600'}`}
            >
              Rooms
            </button>
            <button
              onClick={() => setViewMode('prices')}
              className={`px-2.5 py-1 rounded ${viewMode === 'prices' ? 'bg-white shadow-xs font-semibold text-brand-700' : 'text-neutral-600'}`}
            >
              Prices
            </button>
          </div>
        </div>

        {/* Primary Action Buttons */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowHoldModal(true)}
            className="px-3 py-2 bg-cream-50 hover:bg-cream-100 border border-neutral-300 text-neutral-900 text-xs font-medium rounded-lg flex items-center gap-1.5 transition-colors"
            title="Hold rooms for maintenance or owner use (Shortcut: H)"
          >
            <Lock className="w-3.5 h-3.5 text-neutral-600" />
            <span>Hold rooms</span>
            <kbd className="hidden sm:inline-block px-1 bg-neutral-200 text-[10px] rounded text-neutral-600">H</kbd>
          </button>

          <button
            onClick={() => { setBulkStep(1); setShowBulkPanel(true); }}
            className="px-3.5 py-2 bg-brand-600 hover:bg-brand-700 text-white text-xs font-medium rounded-lg flex items-center gap-1.5 transition-colors"
            title="Change many days at once (Shortcut: B)"
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <span>Change many days</span>
            <kbd className="hidden sm:inline-block px-1 bg-brand-700 text-[10px] rounded text-cream-100">B</kbd>
          </button>
        </div>
      </div>

      {/* Always-visible Legend (Design System 5.2 / Wireframe) */}
      <div className="bg-white px-4 py-2 rounded-lg border border-neutral-300 text-xs flex flex-wrap items-center gap-4 text-neutral-700">
        <span className="font-semibold text-neutral-900">Legend:</span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-xs bg-success-600 inline-block"></span>
          <span>Available</span>
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-xs bg-warning-600 inline-block"></span>
          <span>Few left (&le; 2)</span>
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-xs bg-danger-600 inline-block"></span>
          <span>Sold out</span>
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-xs bg-closed-200 border border-neutral-400 inline-block"></span>
          <span>Closed for sale</span>
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-xs border-2 border-info-600 inline-block"></span>
          <span>Special rule (min stay / CTA)</span>
        </span>
      </div>

      {/* Main Grid */}
      {loading ? (
        <div className="bg-white p-12 text-center rounded-lg border border-neutral-300">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-600 mx-auto"></div>
          <p className="text-xs text-neutral-500 mt-3">Loading calendar matrix...</p>
        </div>
      ) : calendarData && (
        <div className="bg-white rounded-lg border border-neutral-300 overflow-x-auto shadow-xs">
          <table className="w-full border-collapse text-xs select-none">
            {/* Header Dates */}
            <thead>
              <tr className="bg-neutral-100 border-b border-neutral-300 text-neutral-800">
                <th className="py-2.5 px-4 text-left font-semibold sticky left-0 bg-neutral-100 z-10 w-48 border-r border-neutral-300">
                  Rooms &amp; Price Plans
                </th>
                {calendarData.dates.map((dateStr) => {
                  const d = new Date(dateStr);
                  const dayName = d.toLocaleDateString('en-US', { weekday: 'short' });
                  const dayNum = d.getDate();
                  const isWeekend = d.getDay() === 0 || d.getDay() === 6;
                  return (
                    <th
                      key={dateStr}
                      className={`py-2 px-2 text-center border-r border-neutral-200 min-w-[70px] ${
                        isWeekend ? 'bg-cream-100 font-semibold' : ''
                      }`}
                    >
                      <div className="text-[10px] text-neutral-500 uppercase">{dayName}</div>
                      <div className="text-sm font-semibold text-neutral-900">{dayNum}</div>
                    </th>
                  );
                })}
              </tr>
            </thead>

            <tbody>
              {/* Rooms Section */}
              {(viewMode === 'both' || viewMode === 'rooms') && calendarData.rooms.map((room) => (
                <tr key={room.id} className="border-b border-neutral-200">
                  <td className="py-2.5 px-4 sticky left-0 bg-white font-medium text-neutral-900 border-r border-neutral-300 z-10">
                    <div className="font-semibold text-neutral-900">{room.name}</div>
                    <div className="text-[11px] text-neutral-500 font-mono">Total: {room.totalUnits} rooms</div>
                  </td>

                  {calendarData.dates.map((dateStr) => {
                    const day = room.days[dateStr] || { available: room.totalUnits, sold: 0, blocked: 0, statusToken: 'available' };
                    let cellBg = 'bg-success-50 text-success-600 hover:bg-success-600/20';
                    if (day.statusToken === 'soldout') {
                      cellBg = 'bg-danger-50 text-danger-600 hover:bg-danger-600/20';
                    } else if (day.statusToken === 'low') {
                      cellBg = 'bg-warning-50 text-warning-600 hover:bg-warning-600/20';
                    }

                    return (
                      <td
                        key={dateStr}
                        onClick={() => handleOpenEditDay(dateStr, room, undefined)}
                        className={`py-2 px-1 text-center border-r border-neutral-200 cursor-pointer transition-colors ${cellBg}`}
                        title={`${day.available} available (${day.sold} sold, ${day.blocked} held)`}
                      >
                        <div className="font-mono text-sm font-semibold">{day.available}</div>
                        <div className="text-[9px] text-neutral-500">
                          {day.sold > 0 ? `${day.sold} sold` : 'avail'}
                        </div>
                      </td>
                    );
                  })}
                </tr>
              ))}

              {/* Price Plans Section */}
              {(viewMode === 'both' || viewMode === 'prices') && calendarData.ratePlans.map((plan) => (
                <tr key={plan.id} className="border-b border-neutral-200 bg-neutral-50/40">
                  <td className="py-2.5 px-4 sticky left-0 bg-neutral-50 font-medium text-neutral-800 border-r border-neutral-300 z-10">
                    <div className="font-medium text-neutral-900 flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-brand-600"></span>
                      <span>{plan.name}</span>
                    </div>
                    <div className="text-[11px] text-neutral-500 font-mono">{plan.code}</div>
                  </td>

                  {calendarData.dates.map((dateStr) => {
                    const rate = plan.days[dateStr] || { amountFormatted: 4500, stopSell: false, hasRestrictions: false };
                    let cellStyle = 'hover:bg-cream-100 text-neutral-900';
                    if (rate.stopSell) {
                      cellStyle = 'bg-closed-200 text-closed-600';
                    } else if (rate.hasRestrictions) {
                      cellStyle = 'ring-1 ring-inset ring-info-600 text-neutral-900';
                    }

                    return (
                      <td
                        key={dateStr}
                        onClick={() => handleOpenEditDay(dateStr, undefined, plan)}
                        className={`py-2 px-1 text-center border-r border-neutral-200 cursor-pointer transition-colors ${cellStyle}`}
                        title={rate.stopSell ? 'Closed for sale' : `₹${rate.amountFormatted}`}
                      >
                        {rate.stopSell ? (
                          <div className="flex items-center justify-center gap-0.5 text-closed-600">
                            <Lock className="w-3 h-3" />
                            <span className="text-[10px] font-semibold">Closed</span>
                          </div>
                        ) : (
                          <>
                            <div className="font-mono text-xs font-medium">₹{rate.amountFormatted}</div>
                            {rate.minStay > 1 && (
                              <div className="text-[9px] text-info-600 font-medium">min {rate.minStay}n</div>
                            )}
                          </>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Edit Single Day Side Panel (Design System 5.2) */}
      {selectedDayCell && (
        <div className="fixed inset-y-0 right-0 w-96 bg-white border-l border-neutral-300 shadow-2xl z-50 p-6 flex flex-col justify-between overflow-y-auto">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-neutral-200">
              <div>
                <span className="text-[11px] text-neutral-500 font-medium uppercase">Edit Single Day</span>
                <h2 className="text-base font-semibold text-neutral-900">{selectedDayCell.date}</h2>
              </div>
              <button onClick={() => setSelectedDayCell(null)} className="text-neutral-400 hover:text-neutral-700">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mt-4 space-y-4 text-xs">
              {/* Live Preview Banner (Plain-Language Guide Section 4.1) */}
              <div className="p-3 bg-cream-50 border border-cream-200 rounded-lg text-neutral-800">
                <p className="font-semibold text-brand-700">Live Channel Preview:</p>
                <p className="text-[11px] text-neutral-600 mt-0.5">
                  Saving will update Booking.com, Agoda, and all mapped channels within 60s.
                </p>
              </div>

              {selectedDayCell.room && (
                <div>
                  <label className="block font-medium text-neutral-900 mb-1">
                    Rooms available ({selectedDayCell.room.name})
                  </label>
                  <input
                    type="number"
                    min={0}
                    value={editAvailable}
                    onChange={(e) => setEditAvailable(parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:ring-2 focus:ring-brand-600"
                  />
                </div>
              )}

              {selectedDayCell.ratePlan && (
                <>
                  <div>
                    <label className="block font-medium text-neutral-900 mb-1">
                      Standard price (INR)
                    </label>
                    <input
                      type="number"
                      min={1}
                      value={editPrice}
                      onChange={(e) => setEditPrice(parseFloat(e.target.value) || 0)}
                      className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:ring-2 focus:ring-brand-600"
                    />
                  </div>

                  <div className="p-3 bg-neutral-50 rounded-lg border border-neutral-200 space-y-2.5">
                    <label className="flex items-center gap-2 cursor-pointer font-medium text-neutral-900">
                      <input
                        type="checkbox"
                        checked={editStopSell}
                        onChange={(e) => setEditStopSell(e.target.checked)}
                        className="rounded text-brand-600 focus:ring-brand-600 w-4 h-4"
                      />
                      <span>Close this room for sale (Stop-sell)</span>
                    </label>
                    <p className="text-[11px] text-neutral-500 pl-6">
                      Guests cannot book this price plan on this day across all connected channels.
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block font-medium text-neutral-900 mb-1">Minimum nights</label>
                      <input
                        type="number"
                        min={1}
                        value={editMinStay}
                        onChange={(e) => setEditMinStay(parseInt(e.target.value) || 1)}
                        className="w-full px-3 py-2 border border-neutral-400 rounded-lg focus:ring-2 focus:ring-brand-600"
                      />
                    </div>
                  </div>

                  <div className="space-y-2 pt-2 border-t border-neutral-200">
                    <label className="flex items-center gap-2 cursor-pointer text-neutral-800">
                      <input
                        type="checkbox"
                        checked={editCta}
                        onChange={(e) => setEditCta(e.target.checked)}
                        className="rounded text-brand-600"
                      />
                      <span>No check-ins on this day (Closed to arrival)</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer text-neutral-800">
                      <input
                        type="checkbox"
                        checked={editCtd}
                        onChange={(e) => setEditCtd(e.target.checked)}
                        className="rounded text-brand-600"
                      />
                      <span>No check-outs on this day (Closed to departure)</span>
                    </label>
                  </div>
                </>
              )}
            </div>
          </div>

          <div className="pt-4 border-t border-neutral-200 flex justify-end gap-2">
            <button
              onClick={() => setSelectedDayCell(null)}
              className="px-3.5 py-2 border border-neutral-300 rounded-lg text-neutral-700 hover:bg-neutral-100 text-xs"
            >
              Cancel
            </button>
            <button
              onClick={handleSaveDay}
              className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-lg font-medium text-xs flex items-center gap-1.5"
            >
              <span>Save &amp; Update Channels</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* 5-Step Bulk Update Side Panel (Design System 5.3) */}
      {showBulkPanel && (
        <div className="fixed inset-y-0 right-0 w-[420px] bg-white border-l border-neutral-300 shadow-2xl z-50 p-6 flex flex-col justify-between overflow-y-auto">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-neutral-200">
              <div>
                <span className="text-[11px] text-neutral-500 font-medium uppercase">Bulk Update · Step {bulkStep} of 5</span>
                <h2 className="text-base font-semibold text-neutral-900">Change many days</h2>
              </div>
              <button onClick={() => setShowBulkPanel(false)} className="text-neutral-400 hover:text-neutral-700">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mt-4 text-xs space-y-4">
              {/* Step 1: Dates & Weekdays */}
              {bulkStep === 1 && (
                <div className="space-y-4">
                  <h3 className="font-semibold text-neutral-900">1. Select dates and days of the week</h3>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block font-medium text-neutral-700 mb-1">Start date</label>
                      <input
                        type="date"
                        value={bulkStart}
                        onChange={(e) => setBulkStart(e.target.value)}
                        className="w-full px-3 py-2 border border-neutral-400 rounded-lg"
                      />
                    </div>
                    <div>
                      <label className="block font-medium text-neutral-700 mb-1">End date</label>
                      <input
                        type="date"
                        value={bulkEnd}
                        onChange={(e) => setBulkEnd(e.target.value)}
                        className="w-full px-3 py-2 border border-neutral-400 rounded-lg"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block font-medium text-neutral-700 mb-1.5">Apply on weekdays</label>
                    <div className="grid grid-cols-7 gap-1">
                      {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((day, idx) => {
                        const dayNum = idx + 1;
                        const isSelected = selectedWeekdays.includes(dayNum);
                        return (
                          <button
                            key={idx}
                            type="button"
                            onClick={() => {
                              setSelectedWeekdays((prev) =>
                                prev.includes(dayNum) ? prev.filter((d) => d !== dayNum) : [...prev, dayNum]
                              );
                            }}
                            className={`py-1.5 text-xs font-semibold rounded ${
                              isSelected ? 'bg-brand-600 text-white' : 'bg-neutral-100 text-neutral-700'
                            }`}
                          >
                            {day}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>
              )}

              {/* Step 2: Rooms and Prices */}
              {bulkStep === 2 && (
                <div className="space-y-4">
                  <h3 className="font-semibold text-neutral-900">2. Select rooms and price plans</h3>
                  <div>
                    <label className="block font-medium text-neutral-700 mb-1.5">Room Types</label>
                    <div className="space-y-1.5">
                      {calendarData?.rooms.map((r) => (
                        <label key={r.id} className="flex items-center gap-2 cursor-pointer text-neutral-800">
                          <input
                            type="checkbox"
                            checked={selectedBulkRooms.includes(r.id)}
                            onChange={(e) => {
                              setSelectedBulkRooms((prev) =>
                                e.target.checked ? [...prev, r.id] : prev.filter((id) => id !== r.id)
                              );
                            }}
                            className="rounded text-brand-600"
                          />
                          <span>{r.name} ({r.code})</span>
                        </label>
                      ))}
                    </div>
                  </div>

                  <div className="pt-2 border-t border-neutral-200">
                    <label className="block font-medium text-neutral-700 mb-1.5">Price Plans</label>
                    <div className="space-y-1.5">
                      {calendarData?.ratePlans.map((p) => (
                        <label key={p.id} className="flex items-center gap-2 cursor-pointer text-neutral-800">
                          <input
                            type="checkbox"
                            checked={selectedBulkRates.includes(p.id)}
                            onChange={(e) => {
                              setSelectedBulkRates((prev) =>
                                e.target.checked ? [...prev, p.id] : prev.filter((id) => id !== p.id)
                              );
                            }}
                            className="rounded text-brand-600"
                          />
                          <span>{p.name} ({p.code})</span>
                        </label>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* Step 3: Booking Websites */}
              {bulkStep === 3 && (
                <div className="space-y-4">
                  <h3 className="font-semibold text-neutral-900">3. Booking websites</h3>
                  <p className="text-neutral-600">All connected channels will receive this update:</p>
                  <div className="space-y-2 p-3 bg-neutral-50 rounded-lg border border-neutral-200">
                    <label className="flex items-center gap-2 text-neutral-800">
                      <input type="checkbox" defaultChecked disabled className="rounded text-brand-600" />
                      <span className="font-medium">Booking.com</span>
                    </label>
                    <label className="flex items-center gap-2 text-neutral-800">
                      <input type="checkbox" defaultChecked disabled className="rounded text-brand-600" />
                      <span className="font-medium">Agoda</span>
                    </label>
                  </div>
                </div>
              )}

              {/* Step 4: What to Change */}
              {bulkStep === 4 && (
                <div className="space-y-4">
                  <h3 className="font-semibold text-neutral-900">4. What to change</h3>
                  <div>
                    <label className="block font-medium text-neutral-700 mb-1">Standard price (INR)</label>
                    <input
                      type="number"
                      value={bulkPriceValue}
                      onChange={(e) => setBulkPriceValue(parseFloat(e.target.value) || 0)}
                      className="w-full px-3 py-2 border border-neutral-400 rounded-lg"
                    />
                  </div>

                  <div>
                    <label className="block font-medium text-neutral-700 mb-1">Minimum nights</label>
                    <input
                      type="number"
                      min={1}
                      value={bulkMinStay}
                      onChange={(e) => setBulkMinStay(parseInt(e.target.value) || 1)}
                      className="w-full px-3 py-2 border border-neutral-400 rounded-lg"
                    />
                  </div>

                  <label className="flex items-center gap-2 cursor-pointer font-medium text-neutral-900 pt-2">
                    <input
                      type="checkbox"
                      checked={bulkStopSell}
                      onChange={(e) => setBulkStopSell(e.target.checked)}
                      className="rounded text-brand-600 w-4 h-4"
                    />
                    <span>Close rooms for sale on these dates</span>
                  </label>
                </div>
              )}

              {/* Step 5: Review */}
              {bulkStep === 5 && (
                <div className="space-y-4">
                  <h3 className="font-semibold text-neutral-900">5. Review and confirm</h3>
                  <div className="p-4 bg-cream-50 border border-cream-200 rounded-lg space-y-2 text-neutral-800">
                    <p className="font-medium">
                      This changes prices and rules from <span className="font-semibold">{bulkStart}</span> to{' '}
                      <span className="font-semibold">{bulkEnd}</span> across all connected booking websites.
                    </p>
                    <p className="text-[11px] text-neutral-600">
                      You can undo this entire change for up to 24 hours from the activity history.
                    </p>
                  </div>

                  <label className="flex items-start gap-2 cursor-pointer text-neutral-800 pt-2">
                    <input
                      type="checkbox"
                      checked={confirmLargePriceChange}
                      onChange={(e) => setConfirmLargePriceChange(e.target.checked)}
                      className="rounded text-brand-600 mt-0.5"
                    />
                    <span>I confirm this price update across the chosen period.</span>
                  </label>
                </div>
              )}
            </div>
          </div>

          {/* Stepper Navigation */}
          <div className="pt-4 border-t border-neutral-200 flex justify-between gap-2">
            {bulkStep > 1 ? (
              <button
                type="button"
                onClick={() => setBulkStep((s) => s - 1)}
                className="px-3.5 py-2 border border-neutral-300 rounded-lg text-neutral-700 text-xs hover:bg-neutral-100"
              >
                Go back
              </button>
            ) : <div />}

            {bulkStep < 5 ? (
              <button
                type="button"
                onClick={() => setBulkStep((s) => s + 1)}
                className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-lg text-xs font-medium"
              >
                Next step
              </button>
            ) : (
              <button
                type="button"
                onClick={handleApplyBulk}
                className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-lg text-xs font-semibold"
              >
                Apply change
              </button>
            )}
          </div>
        </div>
      )}

      {/* Hold Rooms Modal */}
      {showHoldModal && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-neutral-200">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-100">
              <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                <Lock className="w-4 h-4 text-brand-600" />
                Hold rooms
              </h3>
              <button onClick={() => setShowHoldModal(false)} className="text-neutral-400 hover:text-neutral-700">
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-xs text-neutral-500 mt-2">
              Holding rooms takes them off sale across all connected channels for maintenance or owner use.
            </p>
            <div className="mt-4 space-y-3">
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">Room type</label>
                <select
                  id="hold-room-select"
                  className="w-full px-3 py-2 text-xs border border-neutral-300 rounded-lg bg-white"
                >
                  {calendarData?.rooms.map(r => (
                    <option key={r.id} value={r.id}>{r.name} ({r.code})</option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Start date</label>
                  <input type="date" defaultValue={currentStartDate} id="hold-start" className="w-full px-3 py-2 text-xs border border-neutral-300 rounded-lg" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">End date</label>
                  <input type="date" defaultValue={currentStartDate} id="hold-end" className="w-full px-3 py-2 text-xs border border-neutral-300 rounded-lg" />
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">Rooms to hold</label>
                <input type="number" min="1" defaultValue="1" id="hold-units" className="w-full px-3 py-2 text-xs border border-neutral-300 rounded-lg" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">Reason for hold</label>
                <input type="text" placeholder="e.g. Bathroom maintenance" id="hold-reason" className="w-full px-3 py-2 text-xs border border-neutral-300 rounded-lg" />
              </div>
            </div>
            <div className="mt-6 pt-4 border-t border-neutral-100 flex justify-end gap-2">
              <button onClick={() => setShowHoldModal(false)} className="px-3 py-2 text-xs font-semibold text-neutral-600">Cancel</button>
              <button
                onClick={async () => {
                  const roomEl = document.getElementById('hold-room-select') as HTMLSelectElement;
                  const startEl = document.getElementById('hold-start') as HTMLInputElement;
                  const endEl = document.getElementById('hold-end') as HTMLInputElement;
                  const unitsEl = document.getElementById('hold-units') as HTMLInputElement;
                  const reasonEl = document.getElementById('hold-reason') as HTMLInputElement;
                  try {
                    await apiRequest(`/properties/${selectedPropertyId}/calendar/blocks`, {
                      method: 'POST',
                      body: JSON.stringify({
                        roomTypeId: roomEl.value,
                        startDate: startEl.value,
                        endDate: endEl.value,
                        unitsBlocked: parseInt(unitsEl.value, 10) || 1,
                        reason: reasonEl.value || 'Room hold'
                      })
                    });
                    setShowHoldModal(false);
                    setToastMessage('Rooms held. Outbound availability updated.');
                    loadCalendar(selectedPropertyId, currentStartDate);
                  } catch (err: any) {
                    alert(err?.message || 'Could not hold rooms.');
                  }
                }}
                className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-lg text-xs font-semibold"
              >
                Apply hold
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Undo Toast Notification (Plain-Language Guide Section 4.1) */}
      {toastMessage && (
        <div className="fixed bottom-6 left-6 z-50 bg-neutral-900 text-white px-4 py-3 rounded-lg shadow-xl flex items-center gap-3 text-xs border border-neutral-700 animate-fade-in">
          <Check className="w-4 h-4 text-brand-600" />
          <span>{toastMessage}</span>
          {lastHistoryId && (
            <button
              onClick={handleUndo}
              className="ml-2 font-semibold text-brand-600 hover:underline flex items-center gap-1"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Undo</span>
            </button>
          )}
          <button onClick={() => setToastMessage(null)} className="ml-2 text-neutral-400 hover:text-white">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}
    </div>
  );
};
