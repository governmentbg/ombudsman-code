<div class="m-position">
    <ul class="list">
        @foreach ($period as $p)
            {{-- {{ dd($p->t_path) }} --}}
            <li>
                {{-- {{ dd($snav) }} --}}
                <div class="content">
                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/{{ $prefix }}/{{ $p->t_path }}">
                        {{ $p->t_label }}
                    </a>
                    <div class="m-date"> {{ df(App::getLocale(), $p->t_date, 0) }}</div>
                </div>
                <div class="m-d"></div>

            </li>
        @endforeach
    </ul>
</div>
