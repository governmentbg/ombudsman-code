@extends('layouts.default')
@section('title', $data->MnL_title . ' - ' . trans('common.title'))
@section('site-description', $data->MnL_title)
@section('site-keywords', $data->MnL_title . ' ' . trans('page-keywords'))
@section('site-canonical', '/n/' . $data->MnL_path)
@section('article-headline', $data->MnL_title)
@section('article-date', $data->created_at)
@section('article-modification-date', $data->created_at)
@section('article-description', $data->MnL_title)
@if (count($data->gallery) > 1)
    @section('article-image', '/storage' . $data->cover->ArG_file)
@endif
@section('article-tag', '')


@section('content')

    @include('layouts.breadcrumb')

    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ trans('common.news') }}</h1>
        </div>
    </div>

    {{-- {{ddd($nav["data"])}} --}}
    <div class="m-article">
        {{-- {{ dd($data) }} --}}

        {{-- {{dd($data->gallery->ArG_file)}} --}}

        <h2 class="">{{ $data->MnL_title }}</h2>
        <div class="text-muted mb-3">
            {{ df(App::getLocale(), $data->Mn_date, 0) }}
        </div>



        @if ($data->Mn_embed_video)
            @if (substr($data->Mn_embed_video, 0, 4) == 'http')
                <div class="row">
                    <div class="col-sm-12 col-xs-12 col-md-10 col-lg-8 col-xl-6">
                        <x-embed url="{{ $data->Mn_embed_video }}" class="mb-3" />
                    </div>
                </div>
            @else
                {!! $data->Mn_embed_video !!}
            @endif
        @else
            @if ($data->cover)
                {{-- @if (count($data->gallery) > 1) --}}
                <a href="/storage/{{ $data->cover->ArG_file }}" data-toggle="lightbox" data-gallery="example-gallery">
                    <img src=" /storage/{{ $data->cover->ArG_file }}" class="m-news-image img-fluid" />
                </a>
            @endif
            {{-- @endif --}}
        @endif




        <div class="row justify-content-center mt-5">
            <div class="col-md-10">
                <div class="row">

                    {{-- {{ dd(count($data->gallery)) }} --}}
                    @if (count($data->gallery) > 1)

                        @foreach ($data->gallery as $gal)
                            <a href="/storage/{{ $gal->ArG_file }}" data-toggle="lightbox" data-gallery="example-gallery"
                                class="col-xl-2 col-lg-2 col-md-3 col-sm-3">
                                <div class="img-thumbnail m-lightbox-img"
                                    style="background: url('/storage{{ $gal->ArG_file }}')"></div>

                                {{-- <img src="/storage/{{ $gal->ArG_file }}" class="img-fluid img-thumbnail "> --}}
                            </a>
                        @endforeach

                    @endif


                </div>

            </div>
        </div>

        @if ($data->MnL_intro)
            <div class="m-intro-accent mt-5">
                {{ $data->MnL_intro }}
            </div>
        @endif



        <div class="mt-2">
            {!! $data->MnL_body !!}
        </div>


        <div>
            <ul class="list-unstyled">
                {{-- {{ dd($data->files) }} --}}
                @foreach ($data->files as $n)
                    <li class="mb-2">

                        <span class="m-badge-1">
                            <i class="cil-{{ $n->ArF_type }}"></i>
                        </span>
                        <a href="/storage{{ $n->ArF_file }}">

                            {{ $n->ArF_name }}
                        </a>
                        <span class="m-intro-date">
                            ({{ number_format($n->ArF_size / 1024, 2) }} kb)
                        </span>
                        {{-- ArF_size --}}
                        {{-- ArF_desc --}}
                        {{-- ArF_date --}}
                        @if ($n->ArF_date)
                            <span class="m-intro-date">
                                {{ Carbon\Carbon::createFromDate($n->ArF_date)->format('d/m/Y') }}
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





@stop
