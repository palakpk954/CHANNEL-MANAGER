const API_BASE = '/api/v1';

export interface User {
  id: string;
  organizationId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'SUPER_ADMIN' | 'ORG_ADMIN' | 'PROPERTY_MANAGER' | 'REVENUE_MANAGER' | 'RESERVATIONS_STAFF' | 'READ_ONLY';
  status: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
}

export interface ApiError {
  error: string;
  message: string;
  actionSuggestion?: string;
  status: number;
  timestamp: string;
  validationErrors?: Record<string, string>;
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('access_token');
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const url = endpoint.startsWith(API_BASE) ? endpoint : `${API_BASE}${endpoint}`;
  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorData: ApiError;
    try {
      errorData = await response.json();
    } catch {
      errorData = {
        error: 'NETWORK_ERROR',
        message: 'Could not connect to the channel manager server.',
        actionSuggestion: 'Please check your connection and try again.',
        status: response.status,
        timestamp: new Date().toISOString(),
      };
    }

    if (response.status === 401 && !endpoint.includes('/auth/login')) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      window.location.href = '/login';
    }

    throw errorData;
  }

  return response.json();
}

export const api = {
  get: async <T = any>(endpoint: string) => {
    const data = await apiRequest<T>(endpoint, { method: 'GET' });
    return { data };
  },
  post: async <T = any>(endpoint: string, body?: any) => {
    const data = await apiRequest<T>(endpoint, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    });
    return { data };
  },
  put: async <T = any>(endpoint: string, body?: any) => {
    const data = await apiRequest<T>(endpoint, {
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    });
    return { data };
  },
  delete: async <T = any>(endpoint: string) => {
    const data = await apiRequest<T>(endpoint, { method: 'DELETE' });
    return { data };
  },
};

export default api;

