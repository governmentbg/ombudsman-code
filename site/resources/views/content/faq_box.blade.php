{{-- <div class="m-faq"> --}}
<ul class="list">

    @foreach (\App\Http\Controllers\MFaqController::fqList(App::getLocale(), 10) as $fq)
        <li>
            <a href="{{ lrSeg() }}/{{ App::getLocale() }}/f/{{ $fq->FqL_path }}">
                {{ $fq->FqL_title }}
            </a>
            <div class="m-d"></div>

        </li>
    @endforeach
</ul>
{{-- </div> --}}
