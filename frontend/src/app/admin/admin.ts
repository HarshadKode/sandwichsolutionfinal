import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../api.service';
import { categoryEmoji } from '../emoji';
import { Course, Sandwich, SandwichOrder } from '../models';
import { StatusService } from '../status.service';

@Component({
  selector: 'app-admin',
  imports: [FormsModule, CurrencyPipe],
  templateUrl: './admin.html',
})
export class Admin implements OnInit {
  private readonly api = inject(ApiService);
  private readonly status = inject(StatusService);

  protected readonly sandwiches = signal<Sandwich[]>([]);
  protected readonly categories = computed(() => [
    ...new Set(this.sandwiches().map((s) => s.category).filter(Boolean)),
  ].sort());
  protected readonly prices = signal<Record<number, number>>({});
  protected readonly courses = signal<Course[]>([]);
  /** False when the API has no /api/courses (yet): then the course has to be typed. */
  protected readonly coursesAvailable = signal(false);
  protected readonly printed = signal<string | null>(null);
  protected readonly total = signal<number | null>(null);
  protected readonly emoji = categoryEmoji;

  protected name = '';
  protected category = '';
  protected description = '';
  protected basePrice: number | null = null;

  protected course = '';
  protected orderDate = today();

  async ngOnInit(): Promise<void> {
    await Promise.all([this.load(), this.loadCourses()]);
  }

  protected async loadCourses(): Promise<void> {
    try {
      const list = await firstValueFrom(this.api.findCourses());
      this.courses.set(list);
      this.coursesAvailable.set(true);
      if (!list.some((c) => c.title === this.course)) {
        this.course = list[0]?.title ?? '';
      }
    } catch {
      this.coursesAvailable.set(false); // no /api/courses: fall back to typing the course
    }
  }

  private async load(): Promise<void> {
    try {
      const list = await firstValueFrom(this.api.findSandwiches());
      this.sandwiches.set(list);
      this.prices.set(Object.fromEntries(list.map((s) => [s.id, s.basePrice])));
    } catch (e) {
      this.sandwiches.set([]);
      this.status.error(e);
    }
  }

  protected async add(): Promise<void> {
    try {
      await firstValueFrom(
        this.api.addSandwich({
          name: this.name.trim(),
          description: this.description.trim(),
          category: this.category.trim(),
          basePrice: Number(this.basePrice),
        }),
      );
      this.status.ok(`${this.name} added to the menu.`);
      this.name = this.description = this.category = '';
      this.basePrice = null;
      await this.load();
    } catch (e) {
      this.status.error(e);
    }
  }

  protected setPrice(id: number, value: number): void {
    this.prices.update((p) => ({ ...p, [id]: value }));
  }

  protected async savePrice(s: Sandwich): Promise<void> {
    try {
      await firstValueFrom(this.api.updatePrice(s.id, Number(this.prices()[s.id])));
      this.status.ok(`Price of ${s.name} updated.`);
      await this.load();
    } catch (e) {
      this.status.error(e);
    }
  }

  protected async remove(s: Sandwich): Promise<void> {
    if (!confirm(`Delete ${s.name}?`)) return;
    try {
      await firstValueFrom(this.api.deleteSandwich(s.id));
      this.status.ok(`${s.name} deleted.`);
      await this.load();
    } catch (e) {
      this.status.error(e);
    }
  }

  private order(): SandwichOrder {
    return {
      orderDate: this.orderDate,
      course: { title: this.course.trim() },
    };
  }

  protected async print(): Promise<void> {
    try {
      const text = await firstValueFrom(this.api.printOrder(this.order()));
      this.printed.set(text || 'Order printed.');
    } catch (e) {
      this.status.error(e);
    }
  }

  protected async calculate(): Promise<void> {
    try {
      this.total.set(await firstValueFrom(this.api.orderPrice(this.order())));
    } catch (e) {
      this.status.error(e);
    }
  }
}

/** Today as yyyy-MM-dd. */
function today(): string {
  return new Date().toLocaleDateString('sv-SE');
}
