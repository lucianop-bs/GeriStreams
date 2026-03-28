---
globs: frontend/src/app/**/*.ts, frontend/src/app/**/*.html
---

# Regras Angular 21 — GeriStreams Frontend

## Sintaxe PROIBIDA (causa reprovação na arguição)
- `*ngIf`, `*ngFor`, `*ngSwitch` — templates legados, PROIBIDOS
- `NgModule` com `declarations` — use `standalone: true` em todos os componentes
- `CommonModule` — importe apenas o que usa (`CurrencyPipe`, `DatePipe`, etc.)
- `console.log` — remova antes de qualquer commit
- `CanActivate` como interface de classe — use `CanActivateFn` funcional
- `HttpInterceptor` como interface de classe — use `HttpInterceptorFn`
- `EventEmitter` para estado interno — use `signal()`

## Sintaxe OBRIGATÓRIA

### Componentes
```typescript
@Component({
  selector: 'app-nome',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, NavbarComponent],
  templateUrl: './nome.component.html'
})
export class NomeComponent {
  // Estado via Signals
  readonly items = signal<ItemResponse[]>([]);
  readonly loading = signal(false);
  readonly erro = signal<string | null>(null);

  // Injeção via inject()
  private readonly service = inject(NomeService);

  // Estado derivado via computed()
  readonly total = computed(() => this.items().length);
}
```

### Templates
```html
@if (loading()) {
  <div class="spinner-border text-primary"></div>
} @else if (erro()) {
  <div class="alert alert-danger">{{ erro() }}</div>
} @else {
  @for (item of items(); track item.id) {
    <div>{{ item.nome }}</div>
  } @empty {
    <p class="text-muted">Nenhum item encontrado.</p>
  }
}
```

### Services
```typescript
@Injectable({ providedIn: 'root' })
export class NomeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/recurso`;

  listar(): Observable<ItemResponse[]> {
    return this.http.get<ItemResponse[]>(this.apiUrl);
  }
}
```

### Guards (funcionais)
```typescript
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.isAuthenticated() ? true : router.createUrlTree(['/login']);
};
```

## Rotas
- Todas as rotas devem usar `loadComponent` (lazy loading)
- Rotas protegidas usam `canActivate: [authGuard]`
- Rotas admin usam `canActivate: [authGuard, adminGuard]`

## Estrutura de Arquivos
- `src/app/models/` — interfaces TypeScript correspondentes aos DTOs do backend
- `src/app/services/` — serviços HTTP com `inject(HttpClient)`
- `src/app/components/` — componentes standalone
- `src/app/guards/` — guards funcionais (`CanActivateFn`)
- `src/app/interceptors/` — interceptors funcionais (`HttpInterceptorFn`)
