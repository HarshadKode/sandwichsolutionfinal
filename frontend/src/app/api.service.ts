import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Course, Sandwich, SandwichOrder, SandwichOrderDetail } from './models';

/**
 * All REST endpoints the frontend expects from the Spring Boot API.
 * If your API looks different, this is the only place to change.
 */
export const ENDPOINTS = {
  sandwiches: '/api/sandwiches', // GET (?category=&name=), POST, GET /{id}, DELETE /{id}, PUT /{id}/price
  courses: '/api/courses', // GET (optional: without it the admin panel lets you type the course)
  orderDetails: '/api/orders/details', // POST SandwichOrderDetail
  orderPrint: '/api/orders/print', // POST SandwichOrder
  orderPrice: '/api/orders/price', // POST SandwichOrder -> number
};

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  findSandwiches(category = '', name = ''): Observable<Sandwich[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    if (name) params = params.set('name', name);
    return this.http
      .get<Sandwich | Sandwich[]>(ENDPOINTS.sandwiches, { params })
      .pipe(map((result) => (Array.isArray(result) ? result : result ? [result] : [])));
  }

  addSandwich(sandwich: Omit<Sandwich, 'id'>): Observable<unknown> {
    return this.http.post(ENDPOINTS.sandwiches, sandwich);
  }

  updatePrice(id: number, basePrice: number): Observable<unknown> {
    return this.http.put(`${ENDPOINTS.sandwiches}/${id}/price`, { basePrice });
  }

  deleteSandwich(id: number): Observable<unknown> {
    return this.http.delete(`${ENDPOINTS.sandwiches}/${id}`);
  }

  findCourses(): Observable<Course[]> {
    return this.http.get<Course[]>(ENDPOINTS.courses);
  }

  addOrderDetail(detail: SandwichOrderDetail): Observable<unknown> {
    return this.http.post(ENDPOINTS.orderDetails, detail);
  }

  /** Returns whatever text the API answers (e.g. the printed order), possibly empty. */
  printOrder(order: SandwichOrder): Observable<string> {
    return this.http.post(ENDPOINTS.orderPrint, order, { responseType: 'text' });
  }

  /** Accepts a plain number (`12.5`) or an object like `{ "totalPrice": 12.5 }`. */
  orderPrice(order: SandwichOrder): Observable<number> {
    return this.http.post(ENDPOINTS.orderPrice, order, { responseType: 'text' }).pipe(
      map((text) => {
        let value: unknown = text;
        try {
          value = JSON.parse(text);
        } catch {
          /* plain text */
        }
        if (value && typeof value === 'object') {
          const o = value as Record<string, unknown>;
          value = o['totalPrice'] ?? o['price'] ?? o['total'];
        }
        const n = Number(value);
        if (Number.isNaN(n)) throw new Error(`Unexpected answer from the server: ${text}`);
        return n;
      }),
    );
  }
}
