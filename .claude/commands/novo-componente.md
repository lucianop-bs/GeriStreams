Você é um desenvolvedor frontend sênior do GeriStreams (Angular 21, TypeScript, Bootstrap 5, Signals).

O usuário quer criar um novo componente Angular 21.

## Passo 1 — Coletar Informações

Se não fornecidas, pergunte:

1. **Nome do componente** (ex: `Notificacoes`, `Perfil`, `Relatorio`)
2. **Finalidade**: o que ele exibe/faz?
3. **Operações**: apenas leitura, ou tem formulário de criação/edição?
4. **Proteção de rota**: pública, requer login, ou admin?
5. **Já existe o service Angular?** Se não, criar junto.

## Passo 2 — Verificar arquivos existentes

Leia `frontend/src/app/app.routes.ts` para entender o padrão de rotas existente.
Leia um componente existente como referência de estilo (ex: `subscriptions.component.ts`).

## Passo 3 — Gerar na seguinte ordem

### 1. Model TypeScript (se não existir)
Arquivo: `frontend/src/app/models/{nome}.model.ts`

```typescript
export interface {Nome}Response {
  id: number;
  // campos do backend
  createdAt: string;
}

export interface {Nome}Request {
  // campos do formulário
}
```

### 2. Service Angular (se não existir)
Arquivo: `frontend/src/app/services/{nome}.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class {Nome}Service {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/{rota}`;

  listar(): Observable<{Nome}Response[]> {
    return this.http.get<{Nome}Response[]>(this.apiUrl);
  }
  // criar, atualizar, remover conforme necessário
}
```

### 3. Componente TypeScript
Arquivo: `frontend/src/app/components/{nome}/{nome}.component.ts`

Regras OBRIGATÓRIAS:
- `standalone: true`
- Estado via `signal<T>(valorInicial)`
- Estado derivado via `computed()`
- Injeção via `inject()` (não no construtor)
- `NgOnInit` para carregar dados iniciais

```typescript
@Component({
  selector: 'app-{nome}',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, NavbarComponent, /* outros */],
  templateUrl: './{nome}.component.html'
})
export class {Nome}Component implements OnInit {
  readonly items = signal<{Nome}Response[]>([]);
  readonly loading = signal(false);
  readonly erro = signal<string | null>(null);
  readonly modoEdicao = signal(false);
  readonly itemSelecionado = signal<{Nome}Response | null>(null);

  private readonly service = inject({Nome}Service);
  readonly form = new FormGroup({ /* campos */ });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading.set(true);
    this.service.listar().subscribe({
      next: (data) => { this.items.set(data); this.loading.set(false); },
      error: (e) => { this.erro.set(e.message); this.loading.set(false); }
    });
  }
}
```

### 4. Template HTML
Arquivo: `frontend/src/app/components/{nome}/{nome}.component.html`

Regras OBRIGATÓRIAS:
- USAR: `@if`, `@for (item of items(); track item.id)`, `@empty`
- PROIBIDO: `*ngIf`, `*ngFor`
- UI com Bootstrap 5 (cards, tabelas, modais, badges)
- Feedback visual: spinner de loading, mensagens de erro, estado vazio

```html
<app-navbar></app-navbar>
<div class="container py-4">
  <h2>Título</h2>

  @if (loading()) {
    <div class="text-center py-5">
      <div class="spinner-border text-primary"></div>
    </div>
  } @else if (erro()) {
    <div class="alert alert-danger">{{ erro() }}</div>
  } @else {
    @for (item of items(); track item.id) {
      <div class="card mb-3">
        <!-- conteúdo -->
      </div>
    } @empty {
      <div class="text-center text-muted py-5">
        <p>Nenhum item encontrado.</p>
      </div>
    }
  }
</div>
```

### 5. Rota em app.routes.ts
Adicionar:
```typescript
{
  path: '{nome}',
  loadComponent: () =>
    import('./components/{nome}/{nome}.component')
      .then(m => m.{Nome}Component),
  canActivate: [authGuard] // ou [authGuard, adminGuard] para admin
}
```

## Checklist Final
- [ ] `standalone: true` no componente
- [ ] Nenhum `*ngIf` ou `*ngFor` no template
- [ ] Estado gerenciado com `signal()`
- [ ] Rota com `loadComponent` adicionada
- [ ] Feedback de loading e erro implementado
