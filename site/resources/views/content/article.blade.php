@extends('layouts.default')
@section('title', $data->ArL_title . ' - ' . trans('common.title'))
@section('site-description', $data->Ar_name)
@section('site-keywords', $data->ArL_meta . ' ' . trans('page-keywords'))
@section('site-canonical', '/p/' . $data->ArL_path)
{{-- @section('page-hreflang', $canonical) --}}
@section('article-headline', $data->ArL_title)
@section('article-date', $data->created_at)
@section('article-modification-date', $data->created_at)
@section('article-description', $data->ArL_title)
@section('article-tag', '')


@section('content')

    @include('layouts.breadcrumb')
    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ $data->ArL_title }}</h1>
        </div>
    </div>

    <div class="row">

        @if (Request::segment(1) == 'prava-na-deteto')
            <div class="col-sm-12 col-xs-12 col-xl-9 col-lg-9 col-md-8">
            @else
                <div class="col">
        @endif

        @if ($data->Ar_id == 42)
            @include('content.contact_form')
        @endif

        <div class="mb-5 m-article pt-0">

            {{-- sub navigation --}}
            {{-- {{ dd($subnav) }} --}}
            @if (count($subnav) > 0)
                <div class="m-position">
                    <ul class="list">
                        @foreach ($subnav as $snav)
                            <li>
                                {{-- {{ dd($snav) }} --}}
                                <div class="content">
                                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ $snav->ArL_path }}">
                                        {{ $snav->ArL_title }}
                                    </a>
                                </div>
                                <div class="m-d"></div>

                            </li>
                        @endforeach
                    </ul>
                </div>

            @endif

            @if ($data->Ar_id == 44)
                <div class="m-position">
                    @include('content.faq_box')
                </div>
            @endif



            <div class="m-intro-accent">
                {{ $data->ArL_intro }}
            </div>
            <div class="m-body">
                {!! $data->ArL_body !!}
            </div>
            <div class="mt-5">
                <ul class="list-unstyled m-list">
                    {{-- {{ dd($data->files) }} --}}
                    @foreach ($data->files as $n)
                        <li class="mb-2">

                            <span class="m-badge-1">
                                <i class="cil-{{ $n->ArF_type }}"></i>
                            </span>
                            <a href="/storage{{ $n->ArF_file }}">

                                {{ $n->ArF_name }}
                            </a>
                            @if ($n->ArF_size)
                                <span class="m-intro-date">
                                    ({{ number_format($n->ArF_size / 1024, 2) }} kb)
                                </span>
                            @endif
                            {{-- ArF_size --}}
                            {{-- ArF_desc --}}
                            {{-- ArF_date --}}
                            @if ($n->ArF_date)
                                <span class="m-intro-date">
                                    {{ df(App::getLocale(), $n->ArF_date, 0) }}

                                </span>
                            @endif
                            @if ($n->ArF_desc)
                                <div class="m-intro-accent">
                                    {{ $n->ArF_desc }}
                                </div>
                            @endif


                        </li>
                    @endforeach
                </ul>

            </div>

        </div>
    </div>
    @if (Request::segment(1) != 'lr1' && Request::segment(1) != 'prava-na-deteto')
        @if (count($nav['data']) > 0)
            <div class="col-sm-12 col-xs-12 col-xl-3 col-lg-3 col-md-4">
                <div class="m-faq">
                    <ul class="list">

                        @foreach ($nav['data'] as $n)
                            <li>

                                @if (Request::segment(3) == "$n->ArL_path" or Request::segment(4) == "$n->ArL_path")
                                    <span class="m-cp"> {{ $n->ArL_title }}</span>
                                @else
                                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ $n->ArL_path }}">
                                        {{ $n->ArL_title }}
                                    </a>
                                @endif

                                <div class="m-d"></div>

                            </li>
                        @endforeach
                    </ul>
                </div>
            </div>
        @endif
    @endif
    </div>





@stop
