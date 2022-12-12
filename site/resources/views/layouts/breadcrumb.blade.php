@php
$i = 0;
if (Request::segment(1) == 'lr' or Request::segment(1) == 'prava-na-deteto') {
    $i = 1;
}

if (Request::segment(2 + $i) == 'p') {
    $data = \App\Http\Controllers\NavigationController::metaData(App::getLocale(), Request::segment(3 + $i));
}
@endphp

<nav aria-label="breadcrumb">
    <ol class="breadcrumb">
        <li class="breadcrumb-item"><a href="/{{ App::getLocale() }}">{{ trans('common.home') }}</a></li>

        @if (Request::segment(2 + $i) == 'p')

            @if ($data->parent_cn)
                <li class="breadcrumb-item"><a
                        href="/{{ App::getLocale() }}/{{ Request::segment(2 + $i) }}/{{ $data->parent->ArL_path }}">{{ $data->parent->ArL_title }}</a>
                </li>
            @endif
            <li class="breadcrumb-item active" aria-current="page">{{ $data->ArL_title }}</li>
        @elseif (Request::segment(2 + $i) == 'n')
            <li class="breadcrumb-item active" aria-current="page">{{ trans('common.search.news') }}</li>
        @elseif (Request::segment(2 + $i) == 'e')
            <li class="breadcrumb-item active" aria-current="page">{{ trans('common.search.event') }}</li>
        @elseif (Request::segment(2 + $i) == 'd')
            <li class="breadcrumb-item active" aria-current="page">{{ trans('common.search.position_sc') }}</li>
        @elseif (Request::segment(2 + $i) == 'f')
            <li class="breadcrumb-item active" aria-current="page">{{ trans('common.box_FAQ') }}</li>


        @endif

    </ol>
</nav>
