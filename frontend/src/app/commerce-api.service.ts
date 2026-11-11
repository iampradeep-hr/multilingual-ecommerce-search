import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Product {
  id: string;
  name: string;
  brand: string;
  category: string;
  unit: string;
  price: number;
  mrp: number;
  rating: number;
  reviews: number;
  badge: string;
  emoji: string;
  tint: string;
  tags?: string[];
  languageTags?: Record<string, string[]>;
  stockCities?: string[];
}

export interface Category {
  name: string;
  icon: string;
  tint: string;
}

export interface StoreLocation {
  pincode: string;
  city: string;
  storeName: string;
  deliveryEta: string;
}

export interface ProductDraft {
  name: string;
  brand: string;
  category: string;
  unit: string;
  price: number;
  mrp: number;
  badge: string;
  emoji: string;
  tint: string;
  tags: string[];
  languageTags: Record<string, string[]>;
  stockCities: string[];
}

export type LanguageTagSuggestions = Record<string, string[]>;

@Injectable({ providedIn: 'root' })
export class CommerceApiService {
  private readonly adminCredentialKey = 'mila-admin-credentials';

  constructor(private readonly http: HttpClient) {}

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  search(query: string, category: string, sort: string): Observable<Product[]> {
    const params = new HttpParams()
      .set('q', query)
      .set('category', category)
      .set('sort', sort);
    return this.http.get<Product[]>('/api/search', { params });
  }

  resolveLocation(pincode: string): Observable<StoreLocation> {
    return this.http.get<StoreLocation>('/api/locations/resolve', {
      params: new HttpParams().set('pincode', pincode)
    });
  }

  authenticateAdmin(username: string, password: string): Observable<{ username: string }> {
    const headers = this.basicAuthHeaders(username, password);
    return this.http.get<{ username: string }>('/api/admin/session', { headers });
  }

  saveAdminCredentials(username: string, password: string): void {
    sessionStorage.setItem(this.adminCredentialKey, btoa(`${username}:${password}`));
  }

  clearAdminCredentials(): void {
    sessionStorage.removeItem(this.adminCredentialKey);
  }

  hasAdminCredentials(): boolean {
    return sessionStorage.getItem(this.adminCredentialKey) !== null;
  }

  getAdminProducts(): Observable<Product[]> {
    return this.http.get<Product[]>('/api/admin/products', { headers: this.adminHeaders() });
  }

  addProduct(product: ProductDraft): Observable<Product> {
    return this.http.post<Product>('/api/admin/products', product, { headers: this.adminHeaders() });
  }

  deleteProduct(productId: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/products/${encodeURIComponent(productId)}`, {
      headers: this.adminHeaders()
    });
  }

  previewTags(name: string, brand: string, category: string): Observable<LanguageTagSuggestions> {
    return this.http.post<LanguageTagSuggestions>(
      '/api/admin/products/preview-tags',
      { name, brand, category },
      { headers: this.adminHeaders() }
    );
  }

  generateTags(name: string, brand: string, category: string): Observable<LanguageTagSuggestions> {
    return this.http.post<LanguageTagSuggestions>(
      '/api/admin/products/generate-tags',
      { name, brand, category },
      { headers: this.adminHeaders() }
    );
  }

  private adminHeaders(): HttpHeaders {
    const credentials = sessionStorage.getItem(this.adminCredentialKey);
    if (!credentials) {
      return new HttpHeaders();
    }
    return new HttpHeaders({ Authorization: `Basic ${credentials}` });
  }

  private basicAuthHeaders(username: string, password: string): HttpHeaders {
    return new HttpHeaders({ Authorization: `Basic ${btoa(`${username}:${password}`)}` });
  }
}
