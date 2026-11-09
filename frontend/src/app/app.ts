import { Component, computed, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { Category, CommerceApiService, LanguageTagSuggestions, Product, ProductDraft, StoreLocation } from './commerce-api.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
})
export class App implements OnInit {
  protected readonly tagLanguages = [
    { key: 'english', label: 'English' },
    { key: 'hinglish', label: 'Hinglish' },
    { key: 'hindi', label: 'Hindi' },
    { key: 'marathi', label: 'Marathi' },
    { key: 'bengali', label: 'Bengali' },
    { key: 'assamese', label: 'Assamese' },
    { key: 'gujarati', label: 'Gujarati' },
    { key: 'punjabi', label: 'Punjabi' },
    { key: 'odia', label: 'Odia' },
    { key: 'tamil', label: 'Tamil' },
    { key: 'telugu', label: 'Telugu' },
    { key: 'kannada', label: 'Kannada' },
    { key: 'malayalam', label: 'Malayalam' },
    { key: 'typos', label: 'Typos' },
    { key: 'aiWords', label: 'AI words' }
  ] as const;
  protected readonly storeCities = [
    'Delhi', 'Mumbai', 'Bengaluru', 'Hyderabad', 'Chennai', 'Kolkata', 'Pune', 'Ahmedabad'
  ];
  protected readonly categories = signal<Category[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly location = signal<StoreLocation | null>(null);
  protected readonly selectedCategory = signal('All items');
  protected readonly sortOrder = signal('popular');
  protected readonly loading = signal(false);
  protected readonly locationLoading = signal(false);
  protected readonly categoriesError = signal('');
  protected readonly showCart = signal(false);
  protected readonly showLocationPicker = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly locationError = signal('');
  protected readonly isAdminRoute = window.location.pathname.startsWith('/admin');
  protected readonly isAdminAuthenticated = signal(false);
  protected readonly adminSection = signal<'overview' | 'products' | 'add'>(
    window.location.pathname.endsWith('/new')
      ? 'add'
      : window.location.pathname.endsWith('/products')
        ? 'products'
        : 'overview'
  );
  protected readonly adminProducts = signal<Product[]>([]);
  protected readonly adminLoading = signal(false);
  protected readonly adminSaving = signal(false);
  protected readonly deletingProductId = signal<string | null>(null);
  protected readonly adminError = signal('');
  protected readonly adminNotice = signal('');
  protected readonly suggestedTags = signal<string[]>([]);
  protected readonly tagSuggestionMode = signal<'rules' | 'gemma'>('rules');
  protected readonly aiTagGenerating = signal(false);
  protected readonly aiTagMessage = signal('');
  protected loginUsername = 'root';
  protected loginPassword = '';
  protected loginError = '';
  protected tagInputs: Record<string, string> = {};
  protected productDraft: ProductDraft = this.createProductDraft();
  protected readonly cart = signal<Record<string, { product: Product; quantity: number }>>({});
  protected readonly cartItems = computed(() => Object.values(this.cart()));
  protected readonly cartCount = computed(() => this.cartItems().reduce((count, item) => count + item.quantity, 0));
  protected readonly cartSubtotal = computed(() =>
    this.cartItems().reduce((total, item) => total + item.product.price * item.quantity, 0)
  );

  protected query = '';
  protected pincode = '400001';
  protected pincodeDraft = this.pincode;

  constructor(private readonly commerceApi: CommerceApiService) {}

  ngOnInit(): void {
    if (this.isAdminRoute) {
      this.isAdminAuthenticated.set(this.commerceApi.hasAdminCredentials());
      if (this.isAdminAuthenticated()) {
        this.loadAdminProducts();
        this.loadCategories();
      }
      return;
    }
    this.loadCategories();
    this.loadProducts();
    this.resolveLocation(this.pincode);
  }

  protected loadCategories(): void {
    this.commerceApi.getCategories().subscribe({
      next: (categories) => {
        this.categoriesError.set('');
        this.categories.set([{ name: 'All items', icon: '✳', tint: '#edf5e6' }, ...categories]);
      },
      error: () => {
        this.categoriesError.set('We couldn’t load the store categories. Please try again.');
        this.categories.set([{ name: 'All items', icon: '✳', tint: '#edf5e6' }]);
      }
    });
  }

  protected loadProducts(scrollToResults = false): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.commerceApi
      .search(this.query.trim(), this.selectedCategory(), this.sortOrder())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (products) => {
          this.products.set(products);
          if (scrollToResults) {
            this.scrollToProducts();
          }
        },
        error: () => {
          this.errorMessage.set('Please check that the Spring Boot API is running, then try again.');
          if (scrollToResults) {
            this.scrollToProducts();
          }
        }
      });
  }

  protected retryStoreLoad(): void {
      this.loadCategories();
      this.loadProducts();
  }

  protected search(): void {
    this.selectedCategory.set('All items');
    this.loadProducts(true);
  }

  protected searchFor(query: string): void {
    this.query = query;
    this.selectedCategory.set('All items');
    this.loadProducts(true);
  }

  protected selectCategory(category: string): void {
    this.selectedCategory.set(category);
    this.loadProducts();
    this.scrollToProducts();
  }

  protected changeSort(sort: string): void {
    this.sortOrder.set(sort);
    this.loadProducts();
  }

  protected clearSearch(): void {
    this.query = '';
    this.selectedCategory.set('All items');
    this.loadProducts();
  }

  protected quantityFor(productId: string): number {
    return this.cart()[productId]?.quantity ?? 0;
  }

  protected changeQuantity(product: Product, change: number): void {
    this.cart.update((current) => {
      const next = { ...current };
      const quantity = (next[product.id]?.quantity ?? 0) + change;
      if (quantity <= 0) {
        delete next[product.id];
      } else {
        next[product.id] = { product, quantity };
      }
      return next;
    });
  }

  protected updateLocation(): void {
    this.locationError.set('');
    this.locationLoading.set(true);
    this.commerceApi
      .resolveLocation(this.pincodeDraft.trim())
      .pipe(finalize(() => this.locationLoading.set(false)))
      .subscribe({
        next: (location) => {
          this.pincode = location.pincode;
          this.pincodeDraft = location.pincode;
          this.location.set(location);
          this.showLocationPicker.set(false);
        },
        error: (error: { status?: number }) => {
          this.locationError.set(
            error.status === 400 ? 'Please enter a valid 6-digit PIN code.' : 'We couldn’t check that PIN code. Please try again.'
          );
        }
      });
  }

  protected scrollToProducts(): void {
    document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' });
  }

  protected adminLogin(): void {
    this.loginError = '';
    this.commerceApi.authenticateAdmin(this.loginUsername, this.loginPassword).subscribe({
      next: () => {
        this.commerceApi.saveAdminCredentials(this.loginUsername, this.loginPassword);
        this.isAdminAuthenticated.set(true);
        this.loginPassword = '';
        this.loadAdminProducts();
        this.loadCategories();
      },
      error: () => {
        this.loginError = 'That username or password is not correct.';
      }
    });
  }

  protected adminLogout(): void {
    this.commerceApi.clearAdminCredentials();
    this.isAdminAuthenticated.set(false);
    this.adminProducts.set([]);
  }

  protected setAdminSection(section: 'overview' | 'products' | 'add'): void {
    this.adminSection.set(section);
    this.adminError.set('');
    this.adminNotice.set('');
    const path = section === 'add' ? '/admin/products/new' : section === 'products' ? '/admin/products' : '/admin';
    if (window.location.pathname !== path) {
      window.history.pushState({}, '', path);
    }
    if (section === 'products') {
      this.loadAdminProducts();
    }
    if (section === 'add') {
      this.productDraft = this.createProductDraft();
      this.suggestedTags.set([]);
      this.aiTagMessage.set('');
      this.tagInputs = {};
    }
  }

  protected loadAdminProducts(): void {
    this.adminLoading.set(true);
    this.adminError.set('');
    this.commerceApi
      .getAdminProducts()
      .pipe(finalize(() => this.adminLoading.set(false)))
      .subscribe({
        next: (products) => this.adminProducts.set(products),
        error: (error: { status?: number }) => {
          if (error.status === 401 || error.status === 403) {
            this.adminLogout();
            this.loginError = 'Your admin session expired. Please sign in again.';
            return;
          }
          this.adminError.set('Could not load products from the Spring Boot API. Please try again.');
        }
      });
  }

  protected deleteProduct(product: Product): void {
    if (!window.confirm(`Delete "${product.name}" from the catalog and search?`)) {
      return;
    }

    this.deletingProductId.set(product.id);
    this.adminError.set('');
    this.adminNotice.set('');
    this.commerceApi
      .deleteProduct(product.id)
      .pipe(finalize(() => this.deletingProductId.set(null)))
      .subscribe({
        next: () => {
          this.adminProducts.update((products) => products.filter((item) => item.id !== product.id));
          this.adminNotice.set(`${product.name} was removed from the catalog. Search index cleanup is processing.`);
        },
        error: (error: { status?: number }) => {
          this.adminError.set(error.status === 404
            ? `${product.name} was already removed. Refresh the catalog.`
            : `Could not delete ${product.name}. Please try again.`);
        }
      });
  }

  protected generateTagSuggestions(): void {
    this.adminError.set('');
    this.aiTagMessage.set('');
    this.suggestedTags.set([]);
    this.tagSuggestionMode.set('rules');
    this.commerceApi
      .previewTags(this.productDraft.name, this.productDraft.brand, this.productDraft.category)
      .subscribe({
        next: (suggestions) => {
          this.mergeTagSuggestions(suggestions);
        },
        error: () => this.adminError.set('Could not generate tag suggestions. Please check the API and try again.')
      });
  }

  protected generateAiTagSuggestions(): void {
    this.adminError.set('');
    this.aiTagMessage.set('');
    this.suggestedTags.set([]);
    this.aiTagGenerating.set(true);
    this.aiTagMessage.set('Gemma 3 is generating tags on CPU. The first request can take about two minutes.');
    this.tagSuggestionMode.set('gemma');
    this.commerceApi
      .generateTags(this.productDraft.name, this.productDraft.brand, this.productDraft.category)
      .pipe(finalize(() => this.aiTagGenerating.set(false)))
      .subscribe({
        next: (suggestions) => {
          this.mergeTagSuggestions(suggestions);
          this.aiTagMessage.set('Gemma 3 generated tags. Review them before saving.');
        },
        error: (error: { status?: number }) => {
          this.aiTagMessage.set(error.status === 503
            ? 'Gemma is not configured. Check the Ollama service and backend settings.'
            : error.status === 502
              ? 'Ollama responded, but tag generation failed. Check the backend logs and try again.'
              : 'The backend did not complete tag generation. Check the API connection and try again.');
        }
      });
  }

  protected addLanguageTag(event: Event, language: string): void {
    event.preventDefault();
    const tag = (this.tagInputs[language] ?? '').trim();
    if (!tag) {
      return;
    }
    this.productDraft.languageTags[language] = [
      ...new Set([...(this.productDraft.languageTags[language] ?? []), tag])
    ];
    this.tagInputs[language] = '';
  }

  protected removeLanguageTag(language: string, tag: string): void {
    this.productDraft.languageTags[language] = (this.productDraft.languageTags[language] ?? [])
      .filter((existingTag) => existingTag !== tag);
  }

  private mergeTagSuggestions(suggestions: LanguageTagSuggestions): void {
    for (const [language, tags] of Object.entries(suggestions)) {
      if (language in this.productDraft.languageTags) {
        this.productDraft.languageTags[language] = [
          ...new Set([...(this.productDraft.languageTags[language] ?? []), ...tags])
        ];
      }
    }
    this.suggestedTags.set([...new Set(Object.values(suggestions).flat())]);
  }

  protected languageTagsFor(language: string): string[] {
    return this.productDraft.languageTags[language] ?? [];
  }

  protected setStockCity(city: string, checked: boolean): void {
    const selected = new Set(this.productDraft.stockCities);
    if (checked) {
      selected.add(city);
    } else {
      selected.delete(city);
    }
    this.productDraft.stockCities = [...selected];
  }

  protected isStockedIn(city: string): boolean {
    return this.productDraft.stockCities.includes(city);
  }

  protected addProduct(): void {
    this.adminSaving.set(true);
    this.adminError.set('');
    this.adminNotice.set('');
    this.commerceApi
      .addProduct({
        ...this.productDraft,
        mrp: this.productDraft.mrp || this.productDraft.price,
        tags: [...new Set(Object.values(this.productDraft.languageTags).flat().map((tag) => tag.trim()).filter(Boolean))],
        languageTags: this.productDraft.languageTags,
        stockCities: this.productDraft.stockCities
      })
      .pipe(finalize(() => this.adminSaving.set(false)))
      .subscribe({
        next: (product) => {
          this.adminProducts.update((products) => [product, ...products]);
          this.adminNotice.set(`${product.name} was accepted. Kafka workers are saving it and updating search; it should appear shortly.`);
          this.adminSection.set('products');
          this.productDraft = this.createProductDraft();
        },
        error: (error: { status?: number }) => {
          this.adminError.set(error.status === 400
            ? 'Check the product details. Name, category, unit, and valid prices are required.'
            : 'Could not save this product. Please try again.');
        }
      });
  }

  protected updateDraftTags(value: string): void {
    this.productDraft.tags = value.split(',').map((tag) => tag.trim()).filter(Boolean);
  }

  private createProductDraft(): ProductDraft {
    return {
      name: '',
      brand: '',
      category: '',
      unit: '',
      price: 0,
      mrp: 0,
      badge: '',
      emoji: '🛍️',
      tint: '#edf2e4',
      tags: [],
      languageTags: Object.fromEntries(this.tagLanguages.map(({ key }) => [key, []])),
      stockCities: [...this.storeCities]
    };
  }

  private resolveLocation(pincode: string): void {
    this.commerceApi.resolveLocation(pincode).subscribe({
      next: (location) => this.location.set(location),
      error: () => this.location.set(null)
    });
  }
}
