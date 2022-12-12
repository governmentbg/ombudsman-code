@extends('layouts.default')
{{-- @section('title', trans('common.title'))
@section('site-description', trans('page-description'))
@section('site-keywords', trans('page-keywords'))
@section('site-canonical', $canonical)
@section('page-hreflang', $canonical)
@section('article-headline', trans('page-h1'))
@section('article-date', $publishDate)
@section('article-modification-date', $publishDate)
@section('article-description', trans('page-description'))
@section('article-tag', $tag) --}}


@section('content')


    <div class="not-found text-center ah-d">
        <i class="cis-search text-danger"></i> {{ trans('common.notFound') }}
    </div>

    {{-- {{dd($data)}} --}}


@stop
